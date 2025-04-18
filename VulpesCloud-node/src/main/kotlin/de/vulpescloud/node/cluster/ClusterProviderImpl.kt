package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.AuthenticationProvider
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.cluster.NodeStateChangeEvent
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.event.EventManagerImpl
import de.vulpescloud.node.utils.JsonUtils.getClusterNode
import java.util.*
import org.json.JSONObject
import org.slf4j.LoggerFactory

class ClusterProviderImpl(
    private val config: NodeConfig,
    private val authenticationProvider: AuthenticationProvider,
    eventManager: EventManager,
) : ClusterProvider {

    private val logger = LoggerFactory.getLogger(ClusterProviderImpl::class.java)
    private val eventManager = eventManager as EventManagerImpl

    override fun nodes(): List<ClusterNode> {
        val nodes = mutableListOf<ClusterNode>()
        getRC()?.getAllHashValues("VULPESCLOUD_NODES")?.forEach {
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
        TODO("Not yet implemented")
    }

    override fun localNode(): ClusterNode {
        return nodeByUUID(config.uuid())!!
    }

    fun markOnline() {
        SelectHeadNodeChannelListener(this)

        if (!localNode().headNode) {
            ClusterHeartbeatScheduler(this).run()
        }

        val node =
            ClusterNode(
                config.name(),
                config.uuid(),
                0,
                NodeStates.ONLINE,
                0,
                0,
                "2.0.0",
                nodeByUUID(config.uuid())!!.headNode,
                config.hostname(),
            )

        eventManager.callGlobal(
            NodeStateChangeEvent(node, nodeByUUID(config.uuid())!!.state, NodeStates.ONLINE),
            RedisChannels.VULPESCLOUD_EVENT_CLUSTER_NodeStateChangeEvent,
        )

        getRC()?.setHashField("VULPESCLOUD_NODES", config.name(), JSONObject(node).toString())
    }

    fun initialize() {
        if (getHeadNode() != null && getHeadNode()?.uuid != config.uuid()) {
            logger.debug(
                "Sending Authentication Request to HeadNode. Using secret ${authenticationProvider.getAuthenticationToken()}"
            )

            TemporaryAuthenticationListener(config, eventManager)

            getRC()
                ?.sendMessage(
                    JSONObject()
                        .put("nodeName", config.name())
                        .put("nodeUUID", config.uuid())
                        .put("secret", authenticationProvider.getAuthenticationToken())
                        .toString(),
                    "VULPESCLOUD_NODEAUTHENTICATION",
                )
        } else if (getHeadNode() == null) {
            logger.debug("No HeadNode is present, marking this node as Head")

            getRC()
                ?.setHashField(
                    "VULPESCLOUD_NODES",
                    config.name(),
                    JSONObject(
                            ClusterNode(
                                config.name(),
                                config.uuid(),
                                0,
                                NodeStates.BOOTING,
                                0,
                                0,
                                "2.0.0",
                                true,
                                config.hostname(),
                            )
                        )
                        .toString(),
                )

            logger.debug("Starting Authentication Listener")
            AuthenticationListener(authenticationProvider)
            HeadNodeClusterHeartbeatScheduler(this)
        }
    }

    fun shutdown() {
        // TODO if the Node is the HeadNode, and there are nodes available then pick a random one
        // and promote it to Head
        getRC()
            ?.setHashField(
                "VULPESCLOUD_NODES",
                config.name(),
                JSONObject(
                        ClusterNode(
                            config.name(),
                            config.uuid(),
                            0,
                            NodeStates.OFFLINE,
                            0,
                            0,
                            "0.0.0",
                            false,
                            config.hostname(),
                        )
                    )
                    .toString(),
            )
    }

    fun switchToHeadNode() {
        getRC()?.setHashField(
            "VULPESCLOUD_NODES",
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
}
