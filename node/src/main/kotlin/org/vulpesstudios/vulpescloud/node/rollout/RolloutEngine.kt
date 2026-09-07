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

import build.buf.gen.vulpescloud.players.v1.connectPlayerRequest
import build.buf.gen.vulpescloud.players.v1.getAllOnlinePlayersRequest
import build.buf.gen.vulpescloud.services.v1.*
import build.buf.gen.vulpescloud.tasks.v1.*
import com.google.protobuf.Duration
import com.google.protobuf.Timestamp
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.cluster.NodeSnapshot
import org.vulpesstudios.vulpescloud.api.cluster.NodeState
import org.vulpesstudios.vulpescloud.api.rollout.RolloutProgress
import org.vulpesstudios.vulpescloud.api.rollout.RolloutStatus
import org.vulpesstudios.vulpescloud.api.rollout.RolloutStrategy
import org.vulpesstudios.vulpescloud.api.services.*
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.api.tasks.rolloutId
import org.vulpesstudios.vulpescloud.api.tasks.withoutRolloutId
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.cluster.ClusterHelper
import org.vulpesstudios.vulpescloud.node.grpc.security.AuthClientInterceptor
import build.buf.gen.vulpescloud.tasks.v1.getByNameRequest as taskGetByNameRequest

class RolloutEngine(private val storage: RolloutStorage = RolloutStorage()) {

    private val logger = LoggerFactory.getLogger("RolloutEngine")

    private val creationGraceMillis = 10_000L
    private val retentionMillis = 5 * 60 * 1000L

    suspend fun reconcile() {
        val all =
            try {
                storage.getAll()
            } catch (e: Exception) {
                logger.error("Failed to load rollouts for reconciliation", e)
                return
            }

        all.filter { it.status.isActive }
            .forEach { progress ->
                try {
                    stepRollout(progress)
                } catch (e: Exception) {
                    logger.error("Rollout ${progress.rolloutId} hit an unexpected error", e)
                    runCatching {
                        markFailed(
                            progress,
                            getTask(progress.taskName),
                            "internal_error: ${e.message}",
                        )
                    }
                }
            }

        all.filter { it.status == RolloutStatus.CANCELLED || it.status == RolloutStatus.FAILED }
            .forEach { progress ->
                runCatching { cleanupTerminalRollout(progress) }
                    .onFailure {
                        if (it.toString().contains("JobCancellationException")) return@onFailure
                        logger.error(
                            "Failed to clean up terminal rollout ${progress.rolloutId}",
                            it,
                        )
                    }
            }

        runCatching { reconcileOrphans(all) }
            .onFailure {
                if (it.toString().contains("JobCancellationException")) return@onFailure
                logger.error("Failed to reconcile orphaned rollout references ", it)
            }
        runCatching { reconcileEmptyRollouts(all) }
            .onFailure {
                if (it.toString().contains("JobCancellationException")) return@onFailure
                logger.error("Failed to clean up stale rollout records", it)
            }
    }

    private suspend fun stepRollout(progress: RolloutProgress) {
        val task = getTask(progress.taskName)
        if (task == null) {
            markFailed(progress, null, "task_not_found")
            return
        }

        if (progress.status == RolloutStatus.PENDING) {
            RolloutEvents.started(progress, task)
        }

        when (progress.options.strategy) {
            RolloutStrategy.ROLLING_REPLACE -> stepRollingReplace(progress, task)
            RolloutStrategy.PASSIVE_DRAIN -> stepPassiveDrain(progress, task)
            RolloutStrategy.IMMEDIATE -> stepImmediate(progress, task)
        }
    }

    private suspend fun stepRollingReplace(progressIn: RolloutProgress, task: Task) {
        val live = getServicesOfTask(task)
        val progress = reconcileLiveDrift(progressIn, live)

        val inFlight = progress.newServiceNames.size - progress.servicesStopped
        if (inFlight > 0) {
            val batchNewNames = progress.newServiceNames.takeLast(inFlight)
            val batchNew = live.filter { it.name() in batchNewNames }
            val allReady =
                batchNew.size == batchNewNames.size &&
                    batchNew.all { it.state == ServiceStates.RUNNING }

            if (!allReady) {
                checkReadinessTimeout(progress, task, batchNew, batchNewNames)
                return
            }

            val batchOldNames = progress.pendingStopServiceNames.take(inFlight)
            val batchOld = live.filter { it.name() in batchOldNames }

            markServicesDraining(batchOld, progress.rolloutId)
            stopServices(batchOld)

            val next =
                progress.copy(
                    status = RolloutStatus.IN_PROGRESS,
                    servicesReady = progress.servicesReady + batchNew.size,
                    servicesStopped = progress.servicesStopped + batchOld.size,
                    pendingStopServiceNames =
                        progress.pendingStopServiceNames - batchOldNames.toSet(),
                )
            RolloutEvents.batchProgress(next, task, batchNew, batchOld)
            finishOrPersist(next, task)
            return
        }

        if (progress.pendingStopServiceNames.isEmpty()) {
            markCompleted(progress, task)
            return
        }

        val batchSize = minOf(progress.options.batchSize, progress.pendingStopServiceNames.size)
        val currentOnline = live.count { it.state != ServiceStates.STOPPED }

        if (
            wouldExceedCapacity(
                task,
                currentOnline,
                batchSize,
                progress.options.bypassMaxServiceCount,
            )
        ) {
            val batchOldNames = progress.pendingStopServiceNames.take(batchSize)
            val batchOld = live.filter { it.name() in batchOldNames }
            markServicesDraining(batchOld, progress.rolloutId)
            stopServices(batchOld)
            val next =
                progress.copy(
                    status = RolloutStatus.IN_PROGRESS,
                    servicesStopped = progress.servicesStopped + batchOld.size,
                    pendingStopServiceNames =
                        progress.pendingStopServiceNames - batchOldNames.toSet(),
                    currentBatchNumber = progress.currentBatchNumber + 1,
                )
            RolloutEvents.batchProgress(next, task, emptyList(), batchOld)
            storage.save(next)
            return
        }

        val started = startBatch(task, batchSize, progress.rolloutId)
        if (started.isEmpty()) return

        val next =
            progress.copy(
                status = RolloutStatus.IN_PROGRESS,
                newServiceNames = progress.newServiceNames + started.map { it.name() },
                servicesStarted = progress.servicesStarted + started.size,
                currentBatchNumber = progress.currentBatchNumber + 1,
            )
        RolloutEvents.batchProgress(next, task, started, emptyList())
        storage.save(next)
    }

    private suspend fun stepPassiveDrain(progressIn: RolloutProgress, task: Task) {
        val live = getServicesOfTask(task)
        val progress = reconcileLiveDrift(progressIn, live)

        val inFlight = progress.newServiceNames.size - progress.servicesStopped
        if (inFlight <= 0) {
            if (progress.pendingStopServiceNames.isEmpty()) {
                markCompleted(progress, task)
                return
            }

            val batchSize = minOf(progress.options.batchSize, progress.pendingStopServiceNames.size)
            val currentOnline = live.count { it.state != ServiceStates.STOPPED }
            if (
                wouldExceedCapacity(
                    task,
                    currentOnline,
                    batchSize,
                    progress.options.bypassMaxServiceCount,
                )
            ) {
                return
            }

            val started = startBatch(task, batchSize, progress.rolloutId)
            if (started.isEmpty()) return

            val next =
                progress.copy(
                    status = RolloutStatus.IN_PROGRESS,
                    newServiceNames = progress.newServiceNames + started.map { it.name() },
                    servicesStarted = progress.servicesStarted + started.size,
                    currentBatchNumber = progress.currentBatchNumber + 1,
                )
            RolloutEvents.batchProgress(next, task, started, emptyList())
            storage.save(next)
            return
        }

        val batchNewNames = progress.newServiceNames.takeLast(inFlight)
        val batchNew = live.filter { it.name() in batchNewNames }
        val allReady =
            batchNew.size == batchNewNames.size &&
                batchNew.all { it.state == ServiceStates.RUNNING }

        val batchOldNames = progress.pendingStopServiceNames.take(inFlight)
        val batchOld = live.filter { it.name() in batchOldNames }

        if (!allReady) {
            checkReadinessTimeout(progress, task, batchNew, batchNewNames)
            return
        }

        if (batchOld.none { it.isDraining() }) {
            markServicesDraining(batchOld, progress.rolloutId)
            val next =
                progress.copy(
                    status = RolloutStatus.DRAINING,
                    servicesReady = progress.servicesReady + batchNew.size,
                )
            storage.save(next)
            RolloutEvents.draining(next, task, batchOld)
            return
        }

        val threshold = progress.options.drainPlayerThreshold ?: 0
        val timeoutMs = durationMillis(progress.options.drainTimeout)
        val now = System.currentTimeMillis()
        val fallbackTarget = batchNew.minByOrNull { it.playerCount }?.name()

        val toStop = batchOld.filter { svc ->
            val since = svc.drainingSince() ?: now
            val timedOut = timeoutMs > 0 && now - since >= timeoutMs
            val underThreshold = svc.playerCount <= threshold
            underThreshold || timedOut
        }

        if (toStop.isEmpty()) return

        if (progress.options.fallbackRemainingPlayers && fallbackTarget != null) {
            toStop
                .filter { it.playerCount > 0 }
                .forEach { svc -> transferRemainingPlayers(svc, fallbackTarget) }
        }

        stopServices(toStop)
        val stoppedNames = toStop.map { it.name() }.toSet()
        val stillDrainingInBatch = batchOldNames.toSet() - stoppedNames
        val nextStatus =
            if (stillDrainingInBatch.isEmpty()) RolloutStatus.IN_PROGRESS
            else RolloutStatus.DRAINING

        val next =
            progress.copy(
                status = nextStatus,
                servicesStopped = progress.servicesStopped + toStop.size,
                pendingStopServiceNames = progress.pendingStopServiceNames - stoppedNames,
            )
        RolloutEvents.batchProgress(next, task, batchNew, toStop)
        finishOrPersist(next, task)
    }

    private suspend fun stepImmediate(progressIn: RolloutProgress, task: Task) {
        val live = getServicesOfTask(task)
        val progress = reconcileLiveDrift(progressIn, live)

        if (progress.newServiceNames.isNotEmpty() || progress.servicesStarted > 0) {
            val leftoverOld = live.filter { it.name() in progress.pendingStopServiceNames }
            if (leftoverOld.isNotEmpty()) {
                markServicesDraining(leftoverOld, progress.rolloutId)
                stopServices(leftoverOld)
            }
            markCompleted(
                progress.copy(
                    servicesStopped =
                        progress.servicesStopped + progress.pendingStopServiceNames.size,
                    pendingStopServiceNames = emptyList(),
                ),
                task,
            )
            return
        }

        if (progress.pendingStopServiceNames.isEmpty()) {
            markCompleted(progress, task)
            return
        }

        val currentOnline = live.count { it.state != ServiceStates.STOPPED }
        val targetCount = progress.pendingStopServiceNames.size
        if (
            wouldExceedCapacity(
                task,
                currentOnline,
                targetCount,
                progress.options.bypassMaxServiceCount,
            )
        ) {
            markFailed(
                progress,
                task,
                "capacity_exceeded: immediate rollout of $targetCount service(s) would exceed " +
                    "task.maxOnlineServices, retry with --force/bypassMaxServiceCount",
            )
            return
        }

        val started = startBatch(task, targetCount, progress.rolloutId)
        val batchOld = live.filter { it.name() in progress.pendingStopServiceNames }
        markServicesDraining(batchOld, progress.rolloutId)
        stopServices(batchOld)

        val next =
            progress.copy(
                newServiceNames = progress.newServiceNames + started.map { it.name() },
                servicesStarted = progress.servicesStarted + started.size,
                servicesReady = progress.servicesReady + started.size,
                servicesStopped = progress.servicesStopped + batchOld.size,
                pendingStopServiceNames = emptyList(),
                currentBatchNumber = 1,
                totalBatches = 1,
            )
        RolloutEvents.batchProgress(next, task, started, batchOld)
        markCompleted(next, task)
    }

    private suspend fun cleanupTerminalRollout(progress: RolloutProgress) {
        val touched = getAllServicesLive().filter { it.rolloutId() == progress.rolloutId }
        if (touched.isEmpty()) {
            clearTaskRolloutId(progress.taskName, progress.rolloutId)
            return
        }

        val newOnes = touched.filter { it.rolloutGeneration() == RolloutMetadata.GENERATION_NEW }
        val oldOnes = touched.filter { it.rolloutGeneration() != RolloutMetadata.GENERATION_NEW }

        if (progress.status == RolloutStatus.CANCELLED) {
            if (newOnes.isNotEmpty()) {
                stopServices(newOnes)
                deleteServices(newOnes)
            }
            oldOnes
                .filter { it.isDraining() || it.rolloutId() != null }
                .forEach { clearRolloutMetadata(it) }

            getTask(progress.taskName)?.let { RolloutEvents.cancelled(progress, it, newOnes) }
        } else {
            (newOnes + oldOnes).forEach { clearRolloutMetadata(it) }
        }

        clearTaskRolloutId(progress.taskName, progress.rolloutId)
    }

    private suspend fun reconcileOrphans(all: List<RolloutProgress>) {
        val existingIds = all.map { it.rolloutId }.toSet()

        getAllTasksLive().forEach { task ->
            val id = task.rolloutId() ?: return@forEach
            if (id !in existingIds) {
                logger.warn(
                    "Task ${task.name} references rollout $id which no longer exists, clearing attribute"
                )
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = task.withoutRolloutId().toDefinition() }
                )
            }
        }

        getAllServicesLive().forEach { svc ->
            val id = svc.rolloutId() ?: return@forEach
            if (id !in existingIds) {
                logger.warn(
                    "Service ${svc.name()} references rollout $id which no longer exists, clearing metadata"
                )
                clearRolloutMetadata(svc)
            }
        }
    }

    private suspend fun reconcileEmptyRollouts(all: List<RolloutProgress>) {
        val now = System.currentTimeMillis()
        all.filter { it.status.isFinished }
            .forEach { progress ->
                val startedMillis = millisOf(progress.startedAt) ?: 0L
                if (now - startedMillis < creationGraceMillis) return@forEach

                val completedMillis = millisOf(progress.completedAt) ?: startedMillis
                if (now - completedMillis >= retentionMillis) {
                    storage.delete(progress.rolloutId)
                    logger.debug(
                        "Purged finished rollout ${progress.rolloutId} (task ${progress.taskName}) past retention window"
                    )
                }
            }
    }

    private suspend fun reconcileLiveDrift(
        progress: RolloutProgress,
        live: List<Service>,
    ): RolloutProgress {
        var updated = progress
        var changed = false

        val newTagged = live.filter {
            it.rolloutId() == progress.rolloutId &&
                it.rolloutGeneration() == RolloutMetadata.GENERATION_NEW
        }
        val untrackedNew = newTagged.filter { it.name() !in updated.newServiceNames }
        if (untrackedNew.isNotEmpty()) {
            updated =
                updated.copy(
                    newServiceNames = updated.newServiceNames + untrackedNew.map { it.name() },
                    servicesStarted = updated.servicesStarted + untrackedNew.size,
                )
            changed = true
            logger.info(
                "Rollout ${progress.rolloutId}: picked up ${untrackedNew.size} untracked replacement service(s)"
            )
        }

        val liveByName = live.associateBy { it.name() }
        val goneOld =
            updated.pendingStopServiceNames.filter { name ->
                val svc = liveByName[name]
                svc == null || svc.state == ServiceStates.STOPPED
            }
        if (goneOld.isNotEmpty()) {
            updated =
                updated.copy(
                    pendingStopServiceNames = updated.pendingStopServiceNames - goneOld.toSet(),
                    servicesStopped = updated.servicesStopped + goneOld.size,
                )
            changed = true
            logger.info(
                "Rollout ${progress.rolloutId}: reconciled ${goneOld.size} already-stopped old service(s)"
            )
        }

        if (changed) storage.save(updated)
        return updated
    }

    private suspend fun markServicesDraining(services: List<Service>, rolloutId: String) {
        services.forEach { svc ->
            val updated = svc.withRolloutMetadata(rolloutId).withDraining(true)
            Node.instance.localGrpcClient.serviceAPI.updateServiceMeta(
                updateServiceMetaRequest {
                    this.service = svc.toDefinition()
                    this.meta.putAll(updated.metadata)
                }
            )
        }
    }

    private suspend fun tagAsNewReplacement(service: Service, rolloutId: String) {
        val updated = service.withRolloutMetadata(rolloutId, RolloutMetadata.GENERATION_NEW)
        Node.instance.localGrpcClient.serviceAPI.updateServiceMeta(
            updateServiceMetaRequest {
                this.service = service.toDefinition()
                this.meta.putAll(updated.metadata)
            }
        )
    }

    private suspend fun clearRolloutMetadata(service: Service) {
        val updated = service.withoutRolloutMetadata()
        Node.instance.localGrpcClient.serviceAPI.updateServiceMeta(
            updateServiceMetaRequest {
                this.service = service.toDefinition()
                this.meta.putAll(updated.metadata)
            }
        )
    }

    private suspend fun stopServices(services: List<Service>) {
        services.forEach { svc ->
            Node.instance.localGrpcClient.serviceAPI.stopService(
                stopServiceRequest { this.service = svc.toDefinition() }
            )
        }
    }

    private suspend fun deleteServices(services: List<Service>) {
        services.forEach { svc ->
            Node.instance.localGrpcClient.serviceAPI.deleteService(
                deleteServiceRequest { this.service = svc.toDefinition() }
            )
        }
    }

    private suspend fun transferRemainingPlayers(oldService: Service, targetServiceName: String) {
        val players =
            Node.instance.localGrpcClient.playerAPI
                .getAllOnlinePlayers(getAllOnlinePlayersRequest {})
                .onlinePlayersList
                .filter { it.serverServiceName == oldService.name() }

        players.forEach { player ->
            try {
                Node.instance.localGrpcClient.playerActionsAPI.connectPlayer(
                    connectPlayerRequest {
                        this.uuid = player.uuid
                        this.targetServer = targetServiceName
                    }
                )
            } catch (e: Exception) {
                logger.warn(
                    "Failed to transfer player ${player.name} off draining service ${oldService.name()}",
                    e,
                )
            }
        }
    }

    private suspend fun clearTaskRolloutId(taskName: String, rolloutId: String) {
        val task = getTask(taskName) ?: return
        if (task.rolloutId() == rolloutId) {
            Node.instance.localGrpcClient.tasksAPI.updateTask(
                updateTaskRequest { this.task = task.withoutRolloutId().toDefinition() }
            )
        }
    }

    private suspend fun startBatch(task: Task, count: Int, rolloutId: String): List<Service> {
        val excludedNodes = mutableSetOf<String>()
        val started = mutableListOf<Service>()
        repeat(count) {
            val svc = startNewService(task, excludedNodes) ?: return@repeat
            tagAsNewReplacement(svc, rolloutId)
            started.add(svc)
        }
        return started
    }

    private suspend fun startNewService(task: Task, excludedNodes: MutableSet<String>): Service? {
        val bestNode = pickBestNode(task, excludedNodes)
        if (bestNode == null) {
            logger.warn(
                "No eligible node found to start a rollout replacement for task ${task.name}, will retry next tick"
            )
            return null
        }
        excludedNodes.add(bestNode.name)

        val request =
            PrepareServiceOnTaskRequest.newBuilder()
                .setTask(task.toDefinition())
                .setAmount(1)
                .setMemory(task.maxMemory)
                .setNodeName(bestNode.name)
                .setStart(true)
                .setStartId(1)
                .build()

        val response =
            if (bestNode.name == Node.instance.configProvider.config.nodeName) {
                Node.instance.localGrpcClient.tasksAPI.prepareServiceOnTask(request)
            } else {
                val channel =
                    Node.instance.clusterProvider.remoteNodes
                        .find { it.endpoint.name == bestNode.name }
                        ?.channel
                if (channel == null) {
                    logger.error(
                        "Unable to reach node ${bestNode.name} to start a rollout replacement service"
                    )
                    return null
                }
                TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub(channel)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))
                    .prepareServiceOnTask(request)
            }

        val created = response.servicesList.firstOrNull() ?: return null
        return Service.fromDefinition(created)
    }

    private suspend fun pickBestNode(task: Task, exclude: Set<String>): NodeSnapshot? {
        return ClusterHelper.getAllNodeSnapshots()
            .asSequence()
            .filter { it.name in task.preferredNodes }
            .filter { it.name !in exclude }
            .filter { it.state == NodeState.ONLINE }
            .filter { it.services.memoryAvailable >= task.maxMemory }
            .maxByOrNull { it.services.memoryAvailable }
    }

    private suspend fun getTask(name: String): Task? {
        return Node.instance.localGrpcClient.tasksAPI
            .getByName(taskGetByNameRequest { this.name = name })
            .taskOrNull
            ?.let { Task.fromDefinition(it) }
    }

    private suspend fun getAllTasksLive(): List<Task> {
        return Node.instance.localGrpcClient.tasksAPI
            .getAllTasks(getAllTasksRequest {})
            .tasksList
            .map { Task.fromDefinition(it) }
    }

    private suspend fun getAllServicesLive(): List<Service> {
        return Node.instance.localGrpcClient.serviceAPI
            .getAllServices(getAllServicesRequest {})
            .servicesList
            .map { Service.fromDefinition(it) }
    }

    private suspend fun getServicesOfTask(task: Task): List<Service> {
        return Node.instance.localGrpcClient.serviceAPI
            .getByTask(getByTaskRequest { this.task = task.toDefinition() })
            .servicesList
            .map { Service.fromDefinition(it) }
    }

    private suspend fun checkReadinessTimeout(
        progress: RolloutProgress,
        task: Task,
        liveBatch: List<Service>,
        expectedNames: List<String>,
    ) {
        val timeoutMs = durationMillis(progress.options.readinessTimeout)
        if (timeoutMs <= 0) return

        val now = System.currentTimeMillis()
        val missing = expectedNames - liveBatch.map { it.name() }.toSet()
        if (missing.isNotEmpty()) {
            markFailed(
                progress,
                task,
                "readiness_timeout: service(s) ${missing.joinToString()} never appeared",
            )
            return
        }

        val overdue = liveBatch.filter {
            it.state != ServiceStates.RUNNING &&
                (it.rolloutBatchStartedAt() ?: now) + timeoutMs < now
        }
        if (overdue.isNotEmpty()) {
            markFailed(
                progress,
                task,
                "readiness_timeout: ${overdue.joinToString { it.name() }} did not become RUNNING in time",
            )
        }
    }

    private suspend fun finishOrPersist(progress: RolloutProgress, task: Task) {
        val stillInFlight = progress.newServiceNames.size - progress.servicesStopped
        if (progress.pendingStopServiceNames.isEmpty() && stillInFlight <= 0) {
            markCompleted(progress, task)
        } else {
            storage.save(progress)
        }
    }

    private suspend fun markCompleted(progress: RolloutProgress, task: Task) {
        val next = progress.copy(status = RolloutStatus.COMPLETED, completedAt = nowTimestamp())
        storage.save(next)
        logger.info(
            "Rollout ${progress.rolloutId} for task ${progress.taskName} completed (${progress.options.strategy})"
        )
        RolloutEvents.completed(next, task)
        clearTaskRolloutId(progress.taskName, progress.rolloutId)
    }

    private suspend fun markFailed(progress: RolloutProgress, task: Task?, reason: String) {
        val next =
            progress.copy(
                status = RolloutStatus.FAILED,
                failureReason = reason,
                completedAt = nowTimestamp(),
            )
        storage.save(next)
        logger.error("Rollout ${progress.rolloutId} for task ${progress.taskName} failed: $reason")
        if (task != null) {
            RolloutEvents.failed(next, task)
        }
        clearTaskRolloutId(progress.taskName, progress.rolloutId)
    }

    private fun wouldExceedCapacity(
        task: Task,
        currentOnline: Int,
        batch: Int,
        bypass: Boolean,
    ): Boolean {
        if (bypass) return false
        if (task.maxOnlineServices <= 0) return false
        return currentOnline + batch > task.maxOnlineServices
    }

    private fun durationMillis(duration: Duration?): Long {
        if (duration == null) return 0L
        return duration.seconds * 1000 + duration.nanos / 1_000_000
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
