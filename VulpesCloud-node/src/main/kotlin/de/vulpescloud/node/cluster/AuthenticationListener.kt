package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.AuthenticationProvider
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.jediswrapper.redis.ChannelListener
import java.util.UUID
import org.json.JSONObject
import org.slf4j.LoggerFactory

class AuthenticationListener(private val authenticationProvider: AuthenticationProvider) :
    ChannelListener("VULPESCLOUD_NODEAUTHENTICATION") {

    private val logger = LoggerFactory.getLogger(AuthenticationListener::class.java)

    override fun onMessage(message: String) {
        val msg = JSONObject(JSONObject(message).getString("message"))
        val nodeName = msg.getString("nodeName")
        val nodeUUID = UUID.fromString(msg.getString("nodeUUID"))
        val secret = msg.getString("secret")

        if (secret == authenticationProvider.getAuthenticationToken()) {
            getRC()
                ?.sendMessage(
                    JSONObject().put("status", "AUTHENTICATED").toString(),
                    "VULPESCLOUD_NODEAUTHENTICATION_$nodeName",
                )
            logger.info("Node {} ({}) has been authenticated successfully!", nodeName, nodeUUID)
        } else {
            getRC()
                ?.sendMessage(
                    JSONObject()
                        .put("status", "UNAUTHORIZED")
                        .put("reason", "Invalid Secret!")
                        .toString(),
                    "VULPESCLOUD_NODEAUTHENTICATION_$nodeName",
                )
            logger.info("Node {} ({}) has been rejected due to invalid secret.", nodeName, nodeUUID)
        }
    }
}
