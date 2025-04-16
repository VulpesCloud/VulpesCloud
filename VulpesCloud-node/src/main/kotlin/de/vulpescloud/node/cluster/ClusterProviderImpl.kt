package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.AuthenticationProvider
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.cluster.NodeStateChangeEvent
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.utils.JsonUtils.getClusterNode
import java.util.*
import org.json.JSONObject
import org.slf4j.LoggerFactory

class ClusterProviderImpl(
    private val config: NodeConfig,
    private val authenticationProvider: AuthenticationProvider,
    private val eventManager: EventManager,
) : ClusterProvider {

    private val logger = LoggerFactory.getLogger(ClusterProviderImpl::class.java)

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
        return nodes().find { it.isHeadNode }
    }

    override fun filterByState(state: NodeStates): List<ClusterNode> {
        TODO("Not yet implemented")
    }

    fun markOnline() {
        val node =
            ClusterNode(
                config.name(),
                config.uuid(),
                0,
                NodeStates.ONLINE,
                0,
                0,
                "2.0.0",
                nodeByUUID(config.uuid())!!.isHeadNode,
                config.hostname(),
            )
        getRC()?.setHashField("VULPESCLOUD_NODES", config.name(), JSONObject(node).toString())
        eventManager.call(NodeStateChangeEvent(node, NodeStates.BOOTING, NodeStates.ONLINE))
    }

    fun initialize() {
        /**
         * The Plan is to check if there is a HeadNode Present in the Cluster if it is not present,
         * we will mark this node as HeadNode if it is present, we want to authenticate, this node
         * will send an auth message into a Redis Channel, where we send the name of the Channel
         * used for the authentication messages, this is so that other Nodes cannot interfere with
         * the authentication as we do not know that this node is a valid node.
         */
        if (getHeadNode() != null && getHeadNode()?.uuid != config.uuid()) {

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
}
