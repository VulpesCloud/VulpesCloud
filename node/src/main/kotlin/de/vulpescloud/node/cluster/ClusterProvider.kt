package de.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.cluster.v2.nodeStateChangeEvent
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
    var currentState: NodeState = NodeState.OFFLINE
        private set

    val currentAttributes: MutableMap<String, String> = mutableMapOf()

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

        // TODO: mTLS
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

    suspend fun getClusterConfig(): ClusterConfig {
        return Node.instance.virtualConfigProvider.getCustomConfigObject<ClusterConfig>(
            "vc_cluster"
        ) ?: throw IllegalStateException("ClusterConfig not found!")
    }
}
