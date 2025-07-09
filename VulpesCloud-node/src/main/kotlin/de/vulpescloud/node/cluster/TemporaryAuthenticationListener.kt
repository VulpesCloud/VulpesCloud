package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.cluster.NodeStateChangeEvent
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.jediswrapper.redis.ChannelListener
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.cluster.ClusterProviderImpl.Companion.makeNodeVersion
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.event.EventManagerImpl
import org.json.JSONObject
import org.slf4j.LoggerFactory

class TemporaryAuthenticationListener(
    private val config: NodeConfig,
    eventManager: EventManager,
    private val node: Node,
) : ChannelListener("VULPESCLOUD_NODEAUTHENTICATION_${config.name()}") {

    private val logger = LoggerFactory.getLogger(TemporaryAuthenticationListener::class.java)
    private val eventManager = eventManager as EventManagerImpl

    override fun onMessage(message: String) {
        logger.info("Received Authentication Response from Head Node!")
        val msg = JSONObject(JSONObject(message).getString("message"))
        if (msg.getString("status") == "AUTHENTICATED") {
            logger.info("<green>Successfully <gray>authenticated with the Head Node!")

            val node =
                ClusterNode(
                    config.name(),
                    config.uuid(),
                    0,
                    NodeStates.BOOTING,
                    0,
                    0,
                    makeNodeVersion(),
                    false,
                    config.hostname(),
                )
            getRC()?.setHashField("VULPESCLOUD:NODES", config.name(), JSONObject(node).toString())
            eventManager.callGlobal(
                NodeStateChangeEvent(node, NodeStates.OFFLINE, NodeStates.BOOTING),
                RedisChannels.VULPESCLOUD_EVENT_CLUSTER_NodeStateChangeEvent,
            )

            this.unregister()
        } else {
            logger.error("Unable to authenticate with the Head Node!")
            logger.error("Reason: ${msg.getString("reason")}")
            NodeShutdown.ctrlCCloud()
        }
    }
}
