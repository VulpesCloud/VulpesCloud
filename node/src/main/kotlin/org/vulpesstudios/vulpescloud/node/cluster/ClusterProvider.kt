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

package org.vulpesstudios.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.cluster.v2.nodeStateChangeEvent
import build.buf.gen.vulpescloud.virtualconfig.v1.createVirtualConfigRequest
import io.netty.handler.ssl.SslContext
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.cluster.ClusterConfig
import org.vulpesstudios.vulpescloud.api.cluster.NodeEndpointDetails
import org.vulpesstudios.vulpescloud.api.cluster.NodeState
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.NodeShutdown
import org.vulpesstudios.vulpescloud.node.event.EventsService
import kotlin.time.Duration.Companion.seconds

class ClusterProvider {

    val remoteNodes = mutableListOf<RemoteNode>()
    private val logger = LoggerFactory.getLogger("ClusterProvider")
    private var sameNodeAlreadyOnline: Boolean = false
    var currentState: NodeState = NodeState.OFFLINE
        private set

    val currentAttributes: MutableMap<String, String> = mutableMapOf()

    suspend fun connectToOtherNodes(sslContext: SslContext? = null) {
        val nodes = getClusterConfig().nodes

        nodes.forEach { node ->
            if (
                node.name == Node.instance.configProvider.config.nodeName ||
                    node.uuid == Node.instance.configProvider.config.uuid
            )
                return@forEach

            val remoteNode = RemoteNode(node)
            remoteNode.reconnect(sslContext)
            remoteNodes.add(remoteNode)
        }
    }

    suspend fun init() {
        val localNode = ClusterHelper.getLocalNodeSnapshot()

        if (localNode.state == NodeState.ONLINE) {
            logger.error("Node with same Name is already online! Stopping in 15 seconds...")
            sameNodeAlreadyOnline = true
            delay(15.seconds)
            NodeShutdown.shutdown()
        }
        sameNodeAlreadyOnline = false

        currentState = NodeState.BOOTING
        NodeSnapshotUpdater.updateLocalNodeSnapshot()

        EventsService.publish(
            nodeStateChangeEvent {
                this.snapshot = localNode.toDefinition()
                this.oldState = localNode.state.toNodeStates()
                this.newState = NodeState.BOOTING.toNodeStates()
            },
            true,
        )
    }

    suspend fun shutdown() {
        if (!sameNodeAlreadyOnline) {
            val localNode = ClusterHelper.getLocalNodeSnapshot()
            currentState = NodeState.OFFLINE
            NodeSnapshotUpdater.updateLocalNodeSnapshot()
            EventsService.publish(
                nodeStateChangeEvent {
                    this.snapshot = localNode.toDefinition()
                    this.oldState = localNode.state.toNodeStates()
                    this.newState = NodeState.OFFLINE.toNodeStates()
                },
                true,
            )
        }
    }

    suspend fun startupDone() {
        // a node that was left in maintenance mode comes back in maintenance mode
        Node.instance.nodeMaintenanceProvider.applyLocalAttribute(forceGet = true)
        currentState = NodeState.ONLINE
        NodeSnapshotUpdater.updateLocalNodeSnapshot()

        NodeSnapshotUpdater.start()
    }

    suspend fun initClusterConfig() {
        Node.instance.localGrpcClient.virtualConfigAPI.createVirtualConfig(
            createVirtualConfigRequest {
                this.name = "vc_cluster"
                this.config =
                    Node.instance.virtualConfigProvider.json.encodeToString(
                        ClusterConfig(
                            listOf(
                                NodeEndpointDetails(
                                    Node.instance.configProvider.config.nodeName,
                                    Node.instance.configProvider.config.uuid,
                                    Node.instance.configProvider.config.grpcHost,
                                    Node.instance.configProvider.config.grpcPort,
                                )
                            ),
                            listOf("127.0.0.1", Node.instance.configProvider.config.grpcHost),
                        )
                    )
            }
        )
    }

    /** True if the node is draining because of a drain command (not because it is shutting down). */
    val isDrainingByCommand: Boolean
        get() =
            currentState == NodeState.DRAINING &&
                currentAttributes[DRAIN_REASON_KEY] == DRAIN_REASON_MAINTENANCE

    /**
     * Marks the node as draining because it is shutting down. If the node is already draining
     * (e.g. through a drain command) the state and its reason are left untouched.
     */
    suspend fun markShutdownDraining() {
        if (currentState == NodeState.DRAINING) return
        markDraining(DRAIN_REASON_SHUTTING_DOWN)
    }

    suspend fun markDraining(reason: String) {
        val localNode = ClusterHelper.getLocalNodeSnapshot()
        currentState = NodeState.DRAINING
        currentAttributes[DRAIN_REASON_KEY] = reason
        NodeSnapshotUpdater.updateLocalNodeSnapshot()
        EventsService.publish(
            nodeStateChangeEvent {
                this.snapshot = localNode.toDefinition()
                this.oldState = localNode.state.toNodeStates()
                this.newState = NodeState.DRAINING.toNodeStates()
            },
            true,
        )
    }

    /** Leaves the draining state again (undrain). */
    suspend fun markOnline() {
        val localNode = ClusterHelper.getLocalNodeSnapshot()
        currentState = NodeState.ONLINE
        currentAttributes.remove(DRAIN_REASON_KEY)
        NodeSnapshotUpdater.updateLocalNodeSnapshot()
        EventsService.publish(
            nodeStateChangeEvent {
                this.snapshot = localNode.toDefinition()
                this.oldState = localNode.state.toNodeStates()
                this.newState = NodeState.ONLINE.toNodeStates()
            },
            true,
        )
    }

    suspend fun getClusterConfig(): ClusterConfig {
        return Node.instance.virtualConfigProvider.getCustomConfigObject<ClusterConfig>(
            "vc_cluster"
        ) ?: throw IllegalStateException("ClusterConfig not found!")
    }

    companion object {
        const val DRAIN_REASON_KEY = "drainReason"
        const val DRAIN_REASON_MAINTENANCE = "MAINTENANCE"
        const val DRAIN_REASON_SHUTTING_DOWN = "SHUTTING_DOWN"
    }
}
