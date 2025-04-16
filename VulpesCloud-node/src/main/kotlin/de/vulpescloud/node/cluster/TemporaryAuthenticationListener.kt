package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.cluster.NodeStateChangeEvent
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.jediswrapper.redis.ChannelListener
import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.config.NodeConfig
import org.json.JSONObject
import org.slf4j.LoggerFactory

class TemporaryAuthenticationListener(
    private val config: NodeConfig,
    private val eventManager: EventManager,
) : ChannelListener("VULPESCLOUD_NODEAUTHENTICATION_${config.name()}") {

    private val logger = LoggerFactory.getLogger(TemporaryAuthenticationListener::class.java)

    override fun onMessage(message: String) {
        val msg = JSONObject(message).getJSONObject("message")
        if (msg.getString("status") == "AUTHENTICATED") {
            logger.info("Successfully authenticated with the Head Node!")

            val node =
                ClusterNode(
                    config.name(),
                    config.uuid(),
                    0,
                    NodeStates.BOOTING,
                    0,
                    0,
                    "2.0.0",
                    false,
                    config.hostname(),
                )
            getRC()?.setHashField("VULPESCLOUD_NODES", config.name(), JSONObject(node).toString())
            eventManager.call(NodeStateChangeEvent(node, NodeStates.OFFLINE, NodeStates.BOOTING))

            this.unregister()
        } else {
            logger.error("Unable to authenticate with the Head Node!")
            NodeShutdown.ctrlCCloud()
        }
    }
}
