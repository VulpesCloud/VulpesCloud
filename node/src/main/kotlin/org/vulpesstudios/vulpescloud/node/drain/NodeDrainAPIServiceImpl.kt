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
import build.buf.gen.vulpescloud.draining.v1.*
import com.google.protobuf.Timestamp
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.vulpesstudios.vulpescloud.api.cluster.NodeState
import org.vulpesstudios.vulpescloud.api.drain.NodeDrainStatus
import org.vulpesstudios.vulpescloud.api.drain.NodeDrainStrategy
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.cluster.ClusterHelper
import org.vulpesstudios.vulpescloud.node.event.EventsService
import org.vulpesstudios.vulpescloud.node.grpc.security.AuthClientInterceptor
import org.vulpesstudios.vulpescloud.node.grpc.security.annotations.RequiresPermission
import org.vulpesstudios.vulpescloud.api.drain.NodeDrainProgress as DrainProgress

class NodeDrainAPIServiceImpl : NodeDrainAPIServiceGrpcKt.NodeDrainAPIServiceCoroutineImplBase() {
    private val storage = NodeDrainStorage()
    private val engine = DrainEngine(storage)

    @RequiresPermission("drain.start")
    override suspend fun startNodeDrain(request: StartNodeDrainRequest): StartNodeDrainResponse {
        val node = request.nodeName
        val local = Node.instance.configProvider.config.nodeName
        val snapshots = ClusterHelper.getAllNodeSnapshots()
        val target =
            snapshots.firstOrNull { it.name == node }
                ?: return startNodeDrainResponse { error = "node_not_found: $node" }
        if (target.state == NodeState.OFFLINE || target.state == NodeState.BOOTING)
            return startNodeDrainResponse { error = "node_offline: $node" }
        if (target.state == NodeState.DRAINING) {
            val reason =
                target.attributes[
                        org.vulpesstudios.vulpescloud.node.cluster.ClusterProvider.DRAIN_REASON_KEY]
            return startNodeDrainResponse {
                error =
                    if (
                        reason ==
                            org.vulpesstudios.vulpescloud.node.cluster.ClusterProvider
                                .DRAIN_REASON_MAINTENANCE
                    )
                        "drain_already_active: $node"
                    else "node_shutting_down: $node"
            }
        }
        val force = request.options.hasForce() && request.options.force
        if (!force && snapshots.count { it.state == NodeState.ONLINE } <= 1)
            return startNodeDrainResponse { error = "last_online_node" }
        if (node != local) {
            val remote =
                Node.instance.clusterProvider.remoteNodes.firstOrNull { it.endpoint.name == node }
                    ?: return startNodeDrainResponse { error = "node_offline: $node" }
            val stub =
                NodeDrainAPIServiceGrpcKt.NodeDrainAPIServiceCoroutineStub(remote.channel!!)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))
            return stub.startNodeDrain(request)
        }
        if (storage.getActive(node) != null)
            return startNodeDrainResponse { error = "drain_already_active: $node" }
        val timeout =
            if (request.options.hasTimeout())
                request.options.timeout.seconds * 1000 + request.options.timeout.nanos / 1_000_000
            else DEFAULT_TIMEOUT_MILLIS
        val strategy = NodeDrainStrategy.fromDefinition(request.options.strategy)
        val services =
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(build.buf.gen.vulpescloud.services.v1.getAllServicesRequest {})
                .servicesList
                .filter {
                    it.node == node &&
                        it.state !=
                            build.buf.gen.vulpescloud.services.v1.ServiceState.SERVICE_STATE_STOPPED
                }
        val now = Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000).build()
        val deadline =
            Timestamp.newBuilder().setSeconds((System.currentTimeMillis() + timeout) / 1000).build()
        val progress =
            DrainProgress(
                node,
                strategy,
                NodeDrainStatus.IN_PROGRESS,
                now,
                deadline,
                playerThreshold =
                    if (request.options.hasPlayerThreshold())
                        request.options.playerThreshold.coerceAtLeast(0)
                    else 0,
                servicesTotal = services.size,
                servicesRemaining = services.size,
                playersRemaining = services.sumOf { it.playerCount },
                drainReason =
                    org.vulpesstudios.vulpescloud.node.cluster.ClusterProvider
                        .DRAIN_REASON_MAINTENANCE,
            )
        Node.instance.clusterProvider.markDraining(
            org.vulpesstudios.vulpescloud.node.cluster.ClusterProvider.DRAIN_REASON_MAINTENANCE
        )
        try {
            storage.save(progress)
        } catch (e: Exception) {
            Node.instance.clusterProvider.markOnline()
            return startNodeDrainResponse { error = "drain_storage_failed: ${e.message}" }
        }
        EventsService.publish(
            nodeDrainStartedEvent {
                snapshot = ClusterHelper.getLocalNodeSnapshot().toDefinition()
                this.strategy = strategy.toDefinition()
            },
            true,
        )
        return startNodeDrainResponse {
            success = true
            drain = progress.toDefinition()
        }
    }

    @RequiresPermission("drain.cancel")
    override suspend fun cancelNodeDrain(request: CancelNodeDrainRequest): CancelNodeDrainResponse {
        val local = Node.instance.configProvider.config.nodeName
        if (request.nodeName != local) {
            val remote =
                Node.instance.clusterProvider.remoteNodes.firstOrNull {
                    it.endpoint.name == request.nodeName
                }
                    ?: return cancelNodeDrainResponse {
                        message = "node_not_found: ${request.nodeName}"
                    }
            val stub =
                NodeDrainAPIServiceGrpcKt.NodeDrainAPIServiceCoroutineStub(remote.channel!!)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))
            return stub.cancelNodeDrain(request)
        }
        return if (engine.cancel(request.nodeName))
            cancelNodeDrainResponse {
                success = true
                message = "Drain cancelled"
            }
        else cancelNodeDrainResponse { message = "no_active_drain" }
    }

    @RequiresPermission("drain.status")
    override suspend fun getNodeDrainStatus(
        request: GetNodeDrainStatusRequest
    ): GetNodeDrainStatusResponse {
        val progress =
            storage.get(request.nodeName)
                ?: throw StatusException(
                    Status.NOT_FOUND.withDescription("No drain found for ${request.nodeName}")
                )
        return getNodeDrainStatusResponse { drain = progress.toDefinition() }
    }

    @RequiresPermission("drain.list")
    override suspend fun listActiveNodeDrains(
        request: ListActiveNodeDrainsRequest
    ): ListActiveNodeDrainsResponse = listActiveNodeDrainsResponse {
        drains.addAll(storage.getActive().map { it.toDefinition() })
    }

    @RequiresPermission("drain.stream")
    override fun streamNodeDrainProgress(
        request: StreamNodeDrainProgressRequest
    ): Flow<build.buf.gen.vulpescloud.draining.v1.NodeDrainProgress> = callbackFlow {
        suspend fun sendLatest() {
            val value = storage.get(request.nodeName) ?: return
            trySend(value.toDefinition())
            if (!value.status.isActive) close()
        }
        sendLatest()
        val jobs =
            listOf(
                EventsService.subscribe<NodeDrainProgressEvent> {
                    if (it.snapshot.name == request.nodeName) sendLatest()
                },
                EventsService.subscribe<NodeDrainStartedEvent> {
                    if (it.snapshot.name == request.nodeName) sendLatest()
                },
                EventsService.subscribe<NodeDrainCompletedEvent> {
                    if (it.snapshot.name == request.nodeName) sendLatest()
                },
                EventsService.subscribe<NodeDrainCancelledEvent> {
                    if (it.snapshot.name == request.nodeName) sendLatest()
                },
                EventsService.subscribe<NodeDrainFailedEvent> {
                    if (it.snapshot.name == request.nodeName) sendLatest()
                },
            )
        awaitClose { jobs.forEach { it.cancel() } }
    }

    companion object {
        private const val DEFAULT_TIMEOUT_MILLIS = 10 * 60 * 1000L
    }
}
