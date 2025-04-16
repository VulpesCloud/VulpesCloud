package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.AuthenticationProvider
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.jediswrapper.redis.ChannelListener
import org.json.JSONObject
import org.slf4j.LoggerFactory

class AuthenticationListener(private val authenticationProvider: AuthenticationProvider) :
    ChannelListener("VULPESCLOUD_NODEAUTHENTICATION") {

    private val logger = LoggerFactory.getLogger(AuthenticationListener::class.java)

    override fun onMessage(message: String) {
        val msg = JSONObject(message).getJSONObject("message")
        val nodeName = msg.getString("nodeName")
        val secret = msg.getString("secret")

        if (secret == authenticationProvider.getAuthenticationToken()) {
            getRC()
                ?.sendMessage(
                    JSONObject().put("status", "AUTHENTICATED").toString(),
                    "VULPESCLOUD_NODEAUTHENTICATION_$nodeName",
                )
            logger.debug("Authenticated Node $nodeName")
        } else {
            getRC()
                ?.sendMessage(
                    JSONObject().put("status", "UNAUTHORIZED").toString(),
                    "VULPESCLOUD_NODEAUTHENTICATION_$nodeName",
                )
            logger.debug("Rejected Node $nodeName")
        }
        // TODO in the future we will add the ability to blacklist nodes by Name and UUID, and we
        // will check for that here
    }
}
