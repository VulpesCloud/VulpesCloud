/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.rollout

import build.buf.gen.vulpescloud.rollout.v1.CancelRolloutRequest
import build.buf.gen.vulpescloud.rollout.v1.CancelRolloutResponse
import build.buf.gen.vulpescloud.rollout.v1.GetRolloutStatusRequest
import build.buf.gen.vulpescloud.rollout.v1.GetRolloutStatusResponse
import build.buf.gen.vulpescloud.rollout.v1.ListActiveRolloutsRequest
import build.buf.gen.vulpescloud.rollout.v1.ListActiveRolloutsResponse
import build.buf.gen.vulpescloud.rollout.v1.RolloutAPIServiceGrpcKt
import build.buf.gen.vulpescloud.rollout.v1.StartRolloutRequest
import build.buf.gen.vulpescloud.rollout.v1.StartRolloutResponse
import build.buf.gen.vulpescloud.rollout.v1.StreamRolloutProgressRequest
import build.buf.gen.vulpescloud.rollout.v1.cancelRolloutResponse
import build.buf.gen.vulpescloud.rollout.v1.getRolloutStatusResponse
import build.buf.gen.vulpescloud.rollout.v1.listActiveRolloutsResponse
import build.buf.gen.vulpescloud.rollout.v1.startRolloutResponse
import build.buf.gen.vulpescloud.rollout.v1.RolloutProgress as ProtoRolloutProgress
import build.buf.gen.vulpescloud.rollout.v1.RolloutStrategy as ProtoRolloutStrategy
import build.buf.gen.vulpescloud.events.v1.RolloutBatchProgressEvent
import build.buf.gen.vulpescloud.events.v1.RolloutCancelledEvent
import build.buf.gen.vulpescloud.events.v1.RolloutCompletedEvent
import build.buf.gen.vulpescloud.events.v1.RolloutDrainingEvent
import build.buf.gen.vulpescloud.events.v1.RolloutFailedEvent
import build.buf.gen.vulpescloud.services.v1.getByTaskRequest
import build.buf.gen.vulpescloud.tasks.v1.getByNameRequest as taskGetByNameRequest
import build.buf.gen.vulpescloud.tasks.v1.taskOrNull
import build.buf.gen.vulpescloud.tasks.v1.updateTaskRequest
import com.google.protobuf.Timestamp
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.rollout.RolloutConfigResolver
import org.vulpesstudios.vulpescloud.api.rollout.RolloutGlobalConfig
import org.vulpesstudios.vulpescloud.api.rollout.RolloutProgress
import org.vulpesstudios.vulpescloud.api.rollout.RolloutStatus
import org.vulpesstudios.vulpescloud.api.rollout.RolloutStrategy
import org.vulpesstudios.vulpescloud.api.rollout.toRolloutStrategy
import org.vulpesstudios.vulpescloud.api.services.*
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.api.tasks.withRolloutId
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.event.EventsService
import org.vulpesstudios.vulpescloud.node.grpc.security.annotations.RequiresPermission
import java.util.UUID

/**
 * CLI/Dashboard-facing entry points for rollouts (Phase 6). This service only validates requests
 * and, for [startRollout], persists the initial [RolloutProgress] record - it deliberately does
 * *not* drive the rollout itself. Any node can serve these RPCs since [RolloutStorage] is a shared
 * database; the actual state machine lives in [RolloutEngine], run exclusively by whichever node
 * currently holds the `rollout-reconciler` Chronyx lease.
 */
class RolloutAPIServiceImpl : RolloutAPIServiceGrpcKt.RolloutAPIServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger("RolloutAPIService")
    private val storage = RolloutStorage()

    @RequiresPermission("rollout.start")
    override suspend fun startRollout(request: StartRolloutRequest): StartRolloutResponse {
        val task =
            getTask(request.taskName)
                ?: return startRolloutResponse {
                    success = false
                    error = "task_not_found: ${request.taskName}"
                }

        val existingActive = storage.getActiveByTask(task.name)
        if (existingActive != null) {
            return startRolloutResponse {
                success = false
                error =
                    "task_already_has_active_rollout: ${existingActive.rolloutId} " +
                        "(status=${existingActive.status})"
            }
        }

        val eligible =
            getServicesOfTask(task).filter {
                !it.isDraining() && it.rolloutId() == null && it.state != ServiceStates.STOPPED
            }

        if (eligible.isEmpty()) {
            return startRolloutResponse {
                success = false
                error =
                    "no_eligible_services: task ${task.name} has no running, non-draining service to replace"
            }
        }

        val globalConfig =
            Node.instance.virtualConfigProvider
                .getCustomConfigObject<RolloutGlobalConfig>(RolloutGlobalConfig.VIRTUAL_CONFIG_NAME)
                ?: RolloutGlobalConfig()

        val effectiveOptions =
            RolloutConfigResolver.resolve(
                strategy =
                    request.options.strategy
                        .takeIf { it != ProtoRolloutStrategy.ROLLOUT_STRATEGY_UNSPECIFIED }
                        ?.toRolloutStrategy(),
                batchSize = if (request.options.hasBatchSize()) request.options.batchSize else null,
                drainPlayerThreshold =
                    if (request.options.hasDrainPlayerThreshold()) request.options.drainPlayerThreshold
                    else null,
                drainTimeout =
                    if (request.options.hasDrainTimeout()) request.options.drainTimeout else null,
                readinessTimeout =
                    if (request.options.hasReadinessTimeout()) request.options.readinessTimeout
                    else null,
                bypassMaxServiceCount =
                    if (request.options.hasBypassMaxServiceCount()) request.options.bypassMaxServiceCount
                    else null,
                fallbackRemainingPlayers =
                    if (request.options.hasFallbackRemainingPlayers())
                        request.options.fallbackRemainingPlayers
                    else null,
                taskAttributes = task.attributes,
                globalConfig = globalConfig,
            )

        val totalBatches =
            if (effectiveOptions.strategy == RolloutStrategy.IMMEDIATE) 1
            else
                kotlin.math.ceil(eligible.size / effectiveOptions.batchSize.toDouble())
                    .toInt()
                    .coerceAtLeast(1)

        val rolloutId = UUID.randomUUID().toString()
        val progress =
            RolloutProgress(
                rolloutId = rolloutId,
                taskName = task.name,
                status = RolloutStatus.PENDING,
                options = effectiveOptions,
                totalServicesToReplace = eligible.size,
                pendingStopServiceNames = eligible.map { it.name() },
                startedAt = nowTimestamp(),
                totalBatches = totalBatches,
            )

        storage.create(progress)

        Node.instance.localGrpcClient.tasksAPI.updateTask(
            updateTaskRequest { this.task = task.withRolloutId(rolloutId).toDefinition() }
        )

        logger.info(
            "Rollout $rolloutId started for task ${task.name}: ${eligible.size} service(s) to replace, strategy=${effectiveOptions.strategy}"
        )

        // Note: RolloutStartedEvent is published by RolloutEngine itself on the first reconciler
        // tick that observes this PENDING record, not here - keeping "the engine owns lifecycle
        // events" a single, consistent rule regardless of which node accepted the StartRollout call.
        return startRolloutResponse {
            success = true
            rollout = progress.toDefinition()
        }
    }

    @RequiresPermission("rollout.status")
    override suspend fun getRolloutStatus(request: GetRolloutStatusRequest): GetRolloutStatusResponse {
        val progress =
            if (request.rolloutId.isNotBlank()) {
                storage.get(request.rolloutId)
                    ?: throw StatusException(
                        Status.NOT_FOUND.withDescription(
                            "No rollout found with id ${request.rolloutId}"
                        )
                    )
            } else if (request.taskName.isNotBlank()) {
                val forTask = storage.getByTask(request.taskName)
                forTask.firstOrNull { it.status.isActive }
                    ?: forTask.maxByOrNull {
                        millisOf(it.completedAt) ?: millisOf(it.startedAt) ?: 0L
                    }
                    ?: throw StatusException(
                        Status.NOT_FOUND.withDescription(
                            "No rollout found for task ${request.taskName}"
                        )
                    )
            } else {
                throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription(
                        "Either rollout_id or task_name must be provided"
                    )
                )
            }

        return getRolloutStatusResponse { rollout = progress.toDefinition() }
    }

    @RequiresPermission("rollout.list")
    override suspend fun listActiveRollouts(
        request: ListActiveRolloutsRequest
    ): ListActiveRolloutsResponse {
        return listActiveRolloutsResponse { rollouts.addAll(storage.getActive().map { it.toDefinition() }) }
    }

    @RequiresPermission("rollout.cancel")
    override suspend fun cancelRollout(request: CancelRolloutRequest): CancelRolloutResponse {
        val progress =
            storage.get(request.rolloutId)
                ?: return cancelRolloutResponse {
                    success = false
                    message = "rollout_not_found: ${request.rolloutId}"
                }

        if (!progress.status.isActive) {
            return cancelRolloutResponse {
                success = false
                message = "rollout_not_active: current status is ${progress.status}"
            }
        }

        // Just flip the status here - the rollout-reconciler Chronyx task (RolloutEngine) picks up
        // CANCELLED records on its next tick, does the actual cleanup (terminate new services, clear
        // draining flags) and publishes RolloutCancelledEvent once that's done.
        storage.update(progress.rolloutId) {
            it.copy(status = RolloutStatus.CANCELLED, completedAt = nowTimestamp())
        }

        logger.info("Rollout ${progress.rolloutId} for task ${progress.taskName} cancellation requested")

        return cancelRolloutResponse {
            success = true
            message = "Rollout cancellation requested; cleanup will be performed shortly"
        }
    }

    @RequiresPermission("rollout.stream")
    override fun streamRolloutProgress(request: StreamRolloutProgressRequest): Flow<ProtoRolloutProgress> {
        return callbackFlow {
            storage.get(request.rolloutId)?.let { trySend(it.toDefinition()) }

            suspend fun emitLatest() {
                val latest = storage.get(request.rolloutId) ?: return
                trySend(latest.toDefinition())
                if (latest.status.isFinished) close()
            }

            val jobs =
                listOf(
                    EventsService.subscribe<RolloutBatchProgressEvent> {
                        if (it.rolloutId == request.rolloutId) emitLatest()
                    },
                    EventsService.subscribe<RolloutDrainingEvent> {
                        if (it.rolloutId == request.rolloutId) emitLatest()
                    },
                    EventsService.subscribe<RolloutCompletedEvent> {
                        if (it.progress.rolloutId == request.rolloutId) emitLatest()
                    },
                    EventsService.subscribe<RolloutFailedEvent> {
                        if (it.progress.rolloutId == request.rolloutId) emitLatest()
                    },
                    EventsService.subscribe<RolloutCancelledEvent> {
                        if (it.progress.rolloutId == request.rolloutId) emitLatest()
                    },
                )

            awaitClose { jobs.forEach { it.cancel() } }
        }
    }

    private suspend fun getTask(name: String): Task? {
        return Node.instance.localGrpcClient.tasksAPI
            .getByName(taskGetByNameRequest { this.name = name })
            .taskOrNull
            ?.let { Task.fromDefinition(it) }
    }

    private suspend fun getServicesOfTask(task: Task): List<Service> {
        return Node.instance.localGrpcClient.serviceAPI
            .getByTask(getByTaskRequest { this.task = task.toDefinition() })
            .servicesList
            .map { Service.fromDefinition(it) }
    }

    private fun millisOf(timestamp: Timestamp?): Long? {
        if (timestamp == null) return null
        return timestamp.seconds * 1000 + timestamp.nanos / 1_000_000
    }

    private fun nowTimestamp(): Timestamp {
        val millis = System.currentTimeMillis()
        return Timestamp.newBuilder()
            .setSeconds(millis / 1000)
            .setNanos(((millis % 1000) * 1_000_000).toInt())
            .build()
    }
}
