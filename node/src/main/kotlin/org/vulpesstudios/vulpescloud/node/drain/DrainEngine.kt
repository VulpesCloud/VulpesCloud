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
package org.vulpesstudios.vulpescloud.node.drain

import build.buf.gen.vulpescloud.cluster.v2.*
import build.buf.gen.vulpescloud.players.v1.connectPlayerRequest
import build.buf.gen.vulpescloud.players.v1.getAllOnlinePlayersRequest
import build.buf.gen.vulpescloud.players.v1.kickPlayerRequest
import build.buf.gen.vulpescloud.services.v1.ServiceState
import build.buf.gen.vulpescloud.services.v1.getAllServicesRequest
import build.buf.gen.vulpescloud.services.v1.stopServiceRequest
import build.buf.gen.vulpescloud.services.v1.updateServiceMetaRequest
import com.google.protobuf.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.drain.NodeDrainProgress
import org.vulpesstudios.vulpescloud.api.drain.NodeDrainStatus
import org.vulpesstudios.vulpescloud.api.drain.NodeDrainStrategy
import org.vulpesstudios.vulpescloud.api.serversoftware.SoftwareType
import org.vulpesstudios.vulpescloud.api.services.Service
import org.vulpesstudios.vulpescloud.api.services.ServiceStates
import org.vulpesstudios.vulpescloud.api.services.isDraining
import org.vulpesstudios.vulpescloud.api.services.withDraining
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.NodeShutdown
import org.vulpesstudios.vulpescloud.node.cluster.ClusterHelper
import org.vulpesstudios.vulpescloud.node.event.EventsService
import kotlin.time.Duration.Companion.milliseconds

class DrainEngine(private val storage: NodeDrainStorage = NodeDrainStorage()) {
    private val logger = LoggerFactory.getLogger("DrainEngine")

    suspend fun reconcile() {
        val local = Node.instance.configProvider.config.nodeName
        storage
            .getActive()
            .filter { it.nodeName == local }
            .forEach { progress ->
                if (
                    progress.drainReason ==
                        org.vulpesstudios.vulpescloud.node.cluster.ClusterProvider
                            .DRAIN_REASON_SHUTTING_DOWN
                ) {
                    // A process restarted before its operator initiated shutdown finished. Resume
                    // service
                    // reconciliation normally instead of turning that old stop request into
                    // maintenance.
                    storage.save(
                        progress.copy(status = NodeDrainStatus.CANCELLED, completedAt = now())
                    )
                    clearLocalDrainingMetadata(local)
                    return@forEach
                }
                try {
                    step(progress)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    logger.error("Drain of node $local failed", failure)
                    fail(progress, failure.message ?: "internal_error")
                }
            }
    }

    suspend fun closeForShutdown() {
        val local = Node.instance.configProvider.config.nodeName
        val progress = storage.getActive(local) ?: return
        storage.save(progress.copy(status = NodeDrainStatus.CANCELLED, completedAt = now()))
        EventsService.publish(
            nodeDrainCancelledEvent {
                snapshot = ClusterHelper.getLocalNodeSnapshot().toDefinition()
            },
            true,
        )
        val config = Node.instance.nodeMaintenanceProvider.getConfig(forceGet = true)
        if (config.maintenanceOnStopDuringDrain)
            Node.instance.nodeMaintenanceProvider.setMaintenance(local, true)
    }

    /** Drains local services for a plain stop/exit without enabling maintenance. */
    suspend fun drainForShutdown() {
        val local = Node.instance.configProvider.config.nodeName
        val services =
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(getAllServicesRequest {})
                .servicesList
                .filter { it.node == local && it.state != ServiceState.SERVICE_STATE_STOPPED }
        val startedMillis = System.currentTimeMillis()
        val progress =
            NodeDrainProgress(
                nodeName = local,
                strategy = NodeDrainStrategy.PASSIVE,
                status = NodeDrainStatus.IN_PROGRESS,
                startedAt = now(),
                deadline =
                    Timestamp.newBuilder()
                        .setSeconds((startedMillis + SHUTDOWN_TIMEOUT_MILLIS) / 1000)
                        .build(),
                servicesTotal = services.size,
                servicesRemaining = services.size,
                playersRemaining = services.sumOf { it.playerCount },
                drainReason =
                    org.vulpesstudios.vulpescloud.node.cluster.ClusterProvider
                        .DRAIN_REASON_SHUTTING_DOWN,
            )
        storage.save(progress)
        EventsService.publish(
            nodeDrainStartedEvent {
                snapshot = ClusterHelper.getLocalNodeSnapshot().toDefinition()
                strategy = progress.strategy.toDefinition()
            },
            true,
        )
        while (true) {
            val current = storage.getActive(local) ?: return
            step(current, shutdownMode = true)
            val latest = storage.get(local) ?: return
            if (!latest.status.isActive || latest.servicesRemaining == 0) return
            delay(1_000.milliseconds)
        }
    }

    private suspend fun clearLocalDrainingMetadata(local: String) {
        Node.instance.localGrpcClient.serviceAPI
            .getAllServices(getAllServicesRequest {})
            .servicesList
            .map(Service::fromDefinition)
            .filter { it.node == local && it.isDraining() }
            .forEach { service ->
                Node.instance.localGrpcClient.serviceAPI.updateServiceMeta(
                    updateServiceMetaRequest {
                        this.service = service.toDefinition()
                        meta.putAll(service.withDraining(false).metadata)
                    }
                )
            }
    }

    private suspend fun step(progress: NodeDrainProgress, shutdownMode: Boolean = false) {
        val local = Node.instance.configProvider.config.nodeName
        val services =
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(getAllServicesRequest {})
                .servicesList
                .map(Service::fromDefinition)
                .filter { it.node == local && it.state != ServiceStates.STOPPED }
        services.forEach { service ->
            if (!service.isDraining()) {
                Node.instance.localGrpcClient.serviceAPI.updateServiceMeta(
                    updateServiceMetaRequest {
                        this.service = service.toDefinition()
                        meta.putAll(service.withDraining(true).metadata)
                    }
                )
            }
        }
        val now = System.currentTimeMillis()
        val deadline =
            progress.deadline?.let { it.seconds * 1000 + it.nanos / 1_000_000 } ?: Long.MAX_VALUE
        val stop = services.filter {
            progress.strategy == NodeDrainStrategy.IMMEDIATE ||
                it.playerCount <= progress.playerThreshold ||
                now >= deadline
        }
        val forced = progress.strategy == NodeDrainStrategy.IMMEDIATE || now >= deadline
        if (forced) stop.filter { it.playerCount > 0 }.forEach { handlePlayers(it) }
        stop.forEach { service ->
            Node.instance.localGrpcClient.serviceAPI.stopService(
                stopServiceRequest { this.service = service.toDefinition() }
            )
        }
        val remainingServices = services.size - stop.size
        val players = services.filter { it !in stop }.sumOf { it.playerCount }
        val next =
            progress.copy(
                status = NodeDrainStatus.IN_PROGRESS,
                servicesTotal =
                    maxOf(progress.servicesTotal, services.size + progress.servicesStopped),
                servicesStopped = progress.servicesStopped + stop.size,
                servicesRemaining = remainingServices,
                playersRemaining = players,
            )
        storage.save(next)
        if (stop.isNotEmpty()) publishProgress(next)
        if (remainingServices == 0) {
            if (shutdownMode) {
                storage.save(next.copy(status = NodeDrainStatus.COMPLETED, completedAt = now()))
                EventsService.publish(
                    nodeDrainCompletedEvent {
                        snapshot = ClusterHelper.getLocalNodeSnapshot().toDefinition()
                    },
                    true,
                )
            } else complete(next)
        }
    }

    private suspend fun handlePlayers(service: Service) {
        val players =
            Node.instance.localGrpcClient.playerAPI
                .getAllOnlinePlayers(getAllOnlinePlayersRequest {})
                .onlinePlayersList
                .filter { it.serverServiceName == service.name() }
        if (service.task.software.type == SoftwareType.PROXY) {
            players.forEach { player ->
                runCatching {
                    Node.instance.localGrpcClient.playerActionsAPI.kickPlayer(
                        kickPlayerRequest {
                            uuid = player.uuid
                            reason = "This service is shutting down for maintenance"
                        }
                    )
                }
            }
            return
        }
        val snapshots = ClusterHelper.getAllNodeSnapshots()
        val fallback =
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(getAllServicesRequest {})
                .servicesList
                .map(Service::fromDefinition)
                .firstOrNull { target ->
                    target.task.fallback &&
                        target.state == ServiceStates.RUNNING &&
                        target.name() != service.name() &&
                        snapshots.firstOrNull { it.name == target.node }?.state !=
                            org.vulpesstudios.vulpescloud.api.cluster.NodeState.DRAINING
                }
        if (fallback == null) {
            logger.warn("No fallback service available for players on ${service.name()}")
            return
        }
        players.forEach { player ->
            runCatching {
                Node.instance.localGrpcClient.playerActionsAPI.connectPlayer(
                    connectPlayerRequest {
                        uuid = player.uuid
                        targetServer = fallback.name()
                    }
                )
            }
                .onFailure {
                    logger.warn("Could not move ${player.name} from ${service.name()}", it)
                }
        }
    }

    private suspend fun complete(progress: NodeDrainProgress) {
        val done = progress.copy(status = NodeDrainStatus.COMPLETED, completedAt = now())
        storage.save(done)
        val snapshot = ClusterHelper.getLocalNodeSnapshot()
        EventsService.publish(
            nodeDrainCompletedEvent { this.snapshot = snapshot.toDefinition() },
            true,
        )
        Node.instance.nodeMaintenanceProvider.setMaintenance(progress.nodeName, true)
        NodeShutdown.shutdown()
    }

    suspend fun cancel(nodeName: String): Boolean {
        val progress = storage.getActive(nodeName) ?: return false
        storage.save(progress.copy(status = NodeDrainStatus.CANCELLED, completedAt = now()))
        if (nodeName == Node.instance.configProvider.config.nodeName) {
            Node.instance.nodeMaintenanceProvider.setMaintenance(nodeName, false)
            val local =
                Node.instance.localGrpcClient.serviceAPI
                    .getAllServices(getAllServicesRequest {})
                    .servicesList
                    .map(Service::fromDefinition)
            local
                .filter { it.node == nodeName && it.isDraining() }
                .forEach { service ->
                    Node.instance.localGrpcClient.serviceAPI.updateServiceMeta(
                        updateServiceMetaRequest {
                            this.service = service.toDefinition()
                            meta.putAll(service.withDraining(false).metadata)
                        }
                    )
                }
            Node.instance.clusterProvider.markOnline()
        }
        EventsService.publish(
            nodeDrainCancelledEvent {
                snapshot =
                    ClusterHelper.getAllNodeSnapshots().first { it.name == nodeName }.toDefinition()
            },
            true,
        )
        return true
    }

    private suspend fun fail(progress: NodeDrainProgress, reason: String) {
        if (storage.getActive(progress.nodeName) == null) return
        val failed =
            progress.copy(
                status = NodeDrainStatus.FAILED,
                failureReason = reason,
                completedAt = now(),
            )
        storage.save(failed)
        EventsService.publish(
            nodeDrainFailedEvent {
                snapshot = ClusterHelper.getLocalNodeSnapshot().toDefinition()
                this.reason = reason
            },
            true,
        )
    }

    private suspend fun publishProgress(progress: NodeDrainProgress) {
        EventsService.publish(
            nodeDrainProgressEvent {
                snapshot = ClusterHelper.getLocalNodeSnapshot().toDefinition()
                servicesRemaining = progress.servicesRemaining
                playersRemaining = progress.playersRemaining
            },
            true,
        )
    }

    private fun now(): Timestamp =
        Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000).build()

    companion object {
        private const val SHUTDOWN_TIMEOUT_MILLIS = 10 * 60 * 1000L
    }
}
