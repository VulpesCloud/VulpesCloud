package de.vulpescloud.node.cluster

import de.vulpescloud.jediswrapper.redis.ChannelListener
import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.config.NodeConfig
import org.json.JSONObject
import org.slf4j.LoggerFactory

class TemporaryAuthenticationListener(config: NodeConfig) :
    ChannelListener("VULPESCLOUD_NODEAUTHENTICATION_${config.name()}") {

    private val logger = LoggerFactory.getLogger(TemporaryAuthenticationListener::class.java)

    override fun onMessage(message: String) {
        val msg = JSONObject(message).getJSONObject("messages")
        if (msg.getString("status") == "AUTHENTICATED") {
            logger.info("Successfully authenticated with the Head Node!")

            this.unregister()
        } else {
            logger.error("Unable to authenticate with the Head Node!")
            NodeShutdown.ctrlCCloud()
        }
    }
}
