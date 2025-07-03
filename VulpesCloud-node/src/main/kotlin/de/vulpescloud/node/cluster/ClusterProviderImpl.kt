package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.*
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.cluster.NodeStateChangeEvent
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.CloudVersion
import de.vulpescloud.node.HeartBeatScheduler
import de.vulpescloud.node.Node
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.event.EventManagerImpl
import de.vulpescloud.node.utils.JsonUtils.getClusterNode
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*

class ClusterProviderImpl(
    private val config: NodeConfig,
    private val authenticationProvider: AuthenticationProvider,
    eventManager: EventManager,
) : ClusterProvider {

    private val logger = LoggerFactory.getLogger(ClusterProviderImpl::class.java)
    private val eventManager = eventManager as EventManagerImpl

    private val heartbeatScheduler = HeartBeatScheduler(this)

    override fun nodes(): List<ClusterNode> {
        val nodes = mutableListOf<ClusterNode>()
        getRC()?.getAllHashValues("VULPESCLOUD:NODES")?.forEach {
            nodes.add(getClusterNode(JSONObject(it)))
        }

        return nodes
    }

    override fun nodeByUUID(uuid: UUID): ClusterNode? {
        return nodes().find { it.uuid == uuid }
    }

    override fun nodeByName(name: String): ClusterNode? {
        return nodes().find { it.name == name }
    }

    override fun getHeadNode(): ClusterNode? {
        return nodes().find { it.headNode }
    }

    override fun filterByState(state: NodeStates): List<ClusterNode> {
        return nodes().filter { it.state == state }
    }

    override fun localNode(): ClusterNode {
        return nodeByUUID(config.uuid())!!
    }

    fun markOnline() {
        SelectHeadNodeChannelListener(this)

        val node =
            ClusterNode(
                config.name(),
                config.uuid(),
                0,
                NodeStates.ONLINE,
                0,
                0,
                makeNodeVersion(),
                nodeByUUID(config.uuid())!!.headNode,
                config.hostname(),
            )

        eventManager.callGlobal(
            NodeStateChangeEvent(node, nodeByUUID(config.uuid())!!.state, NodeStates.ONLINE),
            RedisChannels.VULPESCLOUD_EVENT_CLUSTER_NodeStateChangeEvent,
        )

        heartbeatScheduler.run()

        getRC()?.setHashField("VULPESCLOUD:NODES", config.name(), JSONObject(node).toString())
    }

    fun initialize() {
        // Hash 'VULPESCLOUD:NODES' == empty -> make this node HeadNode
        // else
        // Hash 'VULPESCLOUD:NODES' != empty && Field 'localNode.name' == empty -> request
        // authentication from HeadNode
        // Hash 'VULPESCLOUD:NODES' != empty && Field 'localNode.name' != empty -> Refuse startup

        if (getHeadNode() == null) {
            logger.info("Redis Hash empty, marking this node as HeadNode!")

            getRC()
                ?.setHashField(
                    "VULPESCLOUD:NODES",
                    config.name(),
                    JSONObject(
                            ClusterNode(
                                config.name(),
                                config.uuid(),
                                0,
                                NodeStates.BOOTING,
                                0,
                                0,
                                makeNodeVersion(),
                                true,
                                config.hostname(),
                            )
                        )
                        .toString(),
                )
            AuthenticationListener(authenticationProvider)
            Node.instance.setupCondition.signalAll()
        } else {
            logger.info("Requesting Authentication from HeadNode &8(&m${getHeadNode()?.name}&8)")
            logger.debug(
                "Sending Authentication Request to HeadNode. Using secret ${authenticationProvider.getAuthenticationToken()}"
            )

            TemporaryAuthenticationListener(config, eventManager, Node.instance)

            getRC()
                ?.sendMessage(
                    JSONObject()
                        .put("nodeName", config.name())
                        .put("nodeUUID", config.uuid())
                        .put("secret", authenticationProvider.getAuthenticationToken())
                        .toString(),
                    "VULPESCLOUD_NODEAUTHENTICATION",
                )
        }
    }

    fun shutdown() {
        // TODO if the Node is the HeadNode, and there are nodes available then pick a random one
        // and promote it to Head
        getRC()
            ?.setHashField(
                "VULPESCLOUD:NODES",
                config.name(),
                JSONObject(
                        ClusterNode(
                            config.name(),
                            config.uuid(),
                            0,
                            NodeStates.OFFLINE,
                            0,
                            0,
                            makeNodeVersion(),
                            false,
                            config.hostname(),
                        )
                    )
                    .toString(),
            )

        heartbeatScheduler.cancel()
    }

    fun switchToHeadNode() {
        getRC()
            ?.setHashField(
                "VULPESCLOUD:NODES",
                config.name(),
                JSONObject(
                        ClusterNode(
                            config.name(),
                            config.uuid(),
                            localNode().runningServices,
                            NodeStates.ONLINE,
                            localNode().currentMemoryUsage,
                            localNode().maxMemoryUsage,
                            localNode().cloudVersion,
                            true,
                            config.hostname(),
                        )
                    )
                    .toString(),
            )
    }

    companion object {
        fun makeNodeVersion(): NodeCloudVersion {
            return NodeCloudVersion(
                version = CloudVersion.getVersion(),
                buildNumber = CloudVersion.getBuildNumber(),
                gitBranch = CloudVersion.getGitBranch(),
                gitCommit = CloudVersion.getGitCommit(),
                fullVersion = CloudVersion.getFullVersion(),
            )
        }
    }
}
