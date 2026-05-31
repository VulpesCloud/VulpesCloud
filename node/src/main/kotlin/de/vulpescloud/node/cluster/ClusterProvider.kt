package de.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.events.v1.nodeStateChangeEvent
import build.buf.gen.vulpescloud.virtualconfig.v1.createVirtualConfigRequest
import de.vulpescloud.api.cluster.ClusterConfig
import de.vulpescloud.api.cluster.NodeEndpointDetails
import de.vulpescloud.api.cluster.NodeState
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.event.EventsService
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class ClusterProvider {

    val remoteNodes = mutableListOf<RemoteNode>()
    private val logger = LoggerFactory.getLogger("ClusterProvider")
    private var sameNodeAlreadyOnline: Boolean = false

    suspend fun connectToOtherNodes() {
        val nodes = getClusterConfig().nodes

        nodes.forEach { node ->
            if (
                node.name == Node.instance.configProvider.config.nodeName ||
                    node.uuid == Node.instance.configProvider.config.uuid
            )
                return@forEach

            val remoteNode = RemoteNode(node)
            remoteNode.reconnect()
            remoteNodes.add(remoteNode)
        }
    }

    suspend fun init() {
        val head = ClusterHelper.getHeadNode()
        val localNode = ClusterHelper.getLocalNode()

        if (localNode.state == NodeState.ONLINE) {
            logger.error("Node with same Name is already online! Stopping in 15 seconds...")
            sameNodeAlreadyOnline = true
            delay(15.seconds)
            NodeShutdown.shutdown()
        }
        sameNodeAlreadyOnline = false

        if (head == null) {
            val localNode =
                ClusterHelper.getLocalNode().copy(state = NodeState.BOOTING, head = true)
            ClusterHelper.updateNode(localNode)
            EventsService.publish(
                nodeStateChangeEvent {
                    this.node = localNode.toDefinition()
                    this.oldState = localNode.state.toNodeStates()
                    this.newState = NodeState.BOOTING.toNodeStates()
                },
                true,
            )
        } else {
            val localNode =
                ClusterHelper.getLocalNode().copy(state = NodeState.BOOTING, head = false)
            ClusterHelper.updateNode(localNode)
            EventsService.publish(
                nodeStateChangeEvent {
                    this.node = localNode.toDefinition()
                    this.oldState = localNode.state.toNodeStates()
                    this.newState = NodeState.BOOTING.toNodeStates()
                },
                true,
            )
        }
    }

    suspend fun shutdown() {
        if (!sameNodeAlreadyOnline) {
            val localNode = ClusterHelper.getLocalNode()
            ClusterHelper.updateNode(localNode.copy(state = NodeState.OFFLINE, head = false))
            EventsService.publish(
                nodeStateChangeEvent {
                    this.node = localNode.toDefinition()
                    this.oldState = localNode.state.toNodeStates()
                    this.newState = NodeState.OFFLINE.toNodeStates()
                },
                true,
            )
            logger.error(
                "HeadNode features have not yet been implemented! (This is not a real problem right now, just reminder for the dev to implement it)"
            )
            //            EventsService.publish(
            //                EventSerializer.encode(
            //                    ChoseNewHeadEvent(
            //                        ClusterHelper.getAllNodes()
            //                            .filter { it.state == NodeState.ONLINE }
            //                            .minByOrNull { it.bootTimestamp } ?: return
            //                    )
            //                ),
            //                true,
            //            )
        }
    }

    suspend fun startupDone() {
        ClusterHelper.updateNodeState(NodeState.ONLINE)

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
                            )
                        )
                    )
            }
        )
    }

    suspend fun getClusterConfig(): ClusterConfig {
        return Node.instance.virtualConfigProvider.getCustomConfigObject<ClusterConfig>(
            "vc_cluster"
        ) ?: throw IllegalStateException("ClusterConfig not found!")
    }
}
