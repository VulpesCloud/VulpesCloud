package de.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.virtualconfig.v1.createVirtualConfigRequest
import de.vulpescloud.api.cluster.ClusterConfig
import de.vulpescloud.api.cluster.NodeEndpointDetails
import de.vulpescloud.api.cluster.NodeState
import de.vulpescloud.api.events.EventSerializer
import de.vulpescloud.api.events.cluster.ChoseNewHeadEvent
import de.vulpescloud.api.events.cluster.NodeStateChangeEvent
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.event.EventsService
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

class ClusterProvider {

    val remoteNodes = mutableListOf<RemoteNode>()
    private val logger = LoggerFactory.getLogger("ClusterProvider")

    suspend fun connectToOtherNodes() {
        val nodes = getClusterConfig().nodes

        nodes.forEach { node ->
            if (
                node.name == Node.instance.configProvider.config.nodeName ||
                    node.uuid == Node.instance.configProvider.config.uuid
            )
                return@forEach

            val remoteNode = RemoteNode(node)
            remoteNode.connect()
            remoteNodes.add(remoteNode)
        }
    }

    suspend fun init() {
        val head = ClusterHelper.getHeadNode()
        val localNode = ClusterHelper.getLocalNode()

        if (localNode.state == NodeState.ONLINE) {
            logger.error("Node with same Name is already online! Stopping in 15 seconds...")
            delay(15.seconds)
            NodeShutdown.shutdown()
        }

        if (head == null) {
            val localNode =
                ClusterHelper.getLocalNode().copy(state = NodeState.BOOTING, head = true)
            ClusterHelper.updateNode(localNode)
            EventsService.publish(
                EventSerializer.encode(
                    NodeStateChangeEvent(localNode, NodeState.OFFLINE, NodeState.BOOTING)
                ),
                true,
            )
        } else {
            val localNode =
                ClusterHelper.getLocalNode().copy(state = NodeState.BOOTING, head = false)
            ClusterHelper.updateNode(localNode)
            EventsService.publish(
                EventSerializer.encode(
                    NodeStateChangeEvent(localNode, NodeState.OFFLINE, NodeState.BOOTING)
                ),
                true,
            )
        }
    }

    suspend fun shutdown() {
        val localNode = ClusterHelper.getLocalNode()
        ClusterHelper.updateNode(localNode.copy(state = NodeState.OFFLINE, head = false))
        EventsService.publish(
            EventSerializer.encode(
                NodeStateChangeEvent(
                    localNode.copy(state = NodeState.OFFLINE),
                    localNode.state,
                    NodeState.OFFLINE,
                )
            ),
            true,
        )
        EventsService.publish(
            EventSerializer.encode(
                ChoseNewHeadEvent(
                    ClusterHelper.getAllNodes()
                        .filter { it.state == NodeState.ONLINE }
                        .minByOrNull { it.bootTimestamp } ?: return
                )
            ),
            true,
        )
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
