package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.redis.ChannelListener
import de.vulpescloud.node.service.NodeCommunicationServiceHandler.handleServiceMessage
import de.vulpescloud.node.utils.JsonUtils.parsePubSubMessage
import org.slf4j.LoggerFactory

class NodeCommunicationChannelListener(private val clusterProvider: ClusterProvider) : ChannelListener(RedisChannels.VULPESCLOUD_NODE_COMMUNICATION.name) {
    private val logger = LoggerFactory.getLogger("TempLogger")
    override fun onMessage(message: String) {
        logger.debug("Received message on channel {}: {}", channel, message)
        val msg = parsePubSubMessage(message)

        logger.debug("Triggered NodeCommunicationChannelListener with message: {}, {}", msg, msg.getString("content"))

        when (msg.getString("content")) {
            "SERVICE" -> {
                if (msg.getString("receiver") == clusterProvider.localNode().name) {
                    handleServiceMessage(msg)
                }
            }
        }

    }
}
