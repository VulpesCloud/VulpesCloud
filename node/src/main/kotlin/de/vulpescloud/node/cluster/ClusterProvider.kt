package de.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.events.v1.nodeStateChangeEvent
import build.buf.gen.vulpescloud.node.v1.ClusterAPIServiceGrpcKt
import build.buf.gen.vulpescloud.node.v1.authenticateNodeRequest
import build.buf.gen.vulpescloud.virtualconfig.v1.createVirtualConfigRequest
import de.vulpescloud.api.cluster.ClusterConfig
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeEndpointDetails
import de.vulpescloud.api.cluster.NodeState
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.cluster.jobs.CheckNodesJob
import de.vulpescloud.node.cluster.jobs.HeartbeatJob
import de.vulpescloud.node.event.EventsService
import io.grpc.ManagedChannelBuilder
import kotlin.time.Duration.Companion.minutes
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
        if (!ClusterHelper.hasDatabaseBeenInitialized()) {
            logger.warn("Cluster database has not been initialized yet! Initializing...")
            val node =
                ClusterNode(
                    Node.instance.configProvider.config.nodeName,
                    Node.instance.configProvider.config.uuid,
                    Node.instance.configProvider.config.grpcHost,
                    Node.instance.configProvider.config.grpcPort,
                    NodeState.OFFLINE,
                    Node.instance.configProvider.config.maxMemory,
                    false,
                    System.currentTimeMillis(),
                    emptyMap(),
                )

            ClusterHelper.updateNode(node)
            logger.info("Cluster database has been initialized!")
        }

        if (!ClusterHelper.isLocalNodeInDatabase()) {
            logger.error(
                "Local node is not registered in the cluster database! Stopping in 15 seconds..."
            )
            sameNodeAlreadyOnline = true
            delay(15.seconds)
            NodeShutdown.shutdown()
            return
        }

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
            try {
                val clusterConfig = getClusterConfig()
                val endpoint = clusterConfig.nodes.first { it.name == head.name }
                val tempChannel =
                    ManagedChannelBuilder.forAddress(endpoint.host, endpoint.port)
                        .usePlaintext()
                        .build()
                val stub = ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineStub(tempChannel)
                val response =
                    stub.authenticateNode(
                        authenticateNodeRequest {
                            this.nodeName = Node.instance.configProvider.config.nodeName
                            this.nodeUuid = Node.instance.configProvider.config.uuid.toString()
                        }
                    )
                if (!response.success) {
                    logger.error(
                        "HeadNode refused authentication due to ${response.message}! Stopping in 15 seconds..."
                    )
                    sameNodeAlreadyOnline = true
                    delay(15.seconds)
                    NodeShutdown.shutdown()
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("Failed to authenticate with HeadNode! Stopping in 15 seconds...")
                sameNodeAlreadyOnline = true
                delay(15.seconds)
                NodeShutdown.shutdown()
                return
            }

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
        }
    }

    suspend fun startupDone() {
        ClusterHelper.updateNodeState(NodeState.ONLINE)

        NodeSnapshotUpdater.start()
        if (ClusterHelper.getLocalNode().head) {
            CheckNodesJob.start()
        } else {
            HeartbeatJob.start()
        }
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
                            60.seconds.toString(),
                            5.minutes.toString(),
                            10.minutes.toString(),
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
