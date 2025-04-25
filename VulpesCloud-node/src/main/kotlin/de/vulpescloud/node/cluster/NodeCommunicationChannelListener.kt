package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.redis.ChannelListener
import de.vulpescloud.node.service.NodeCommunicationServiceHandler.handleServiceMessage
import de.vulpescloud.node.utils.JsonUtils.parsePubSubMessage

class NodeCommunicationChannelListener(private val clusterProvider: ClusterProvider) : ChannelListener(RedisChannels.VULPESCLOUD_NODE_COMMUNICATION.name) {
    override fun onMessage(message: String) {
        val msg = parsePubSubMessage(message)

        when (msg.getString("content")) {
            "SERVICE" -> {
                if (msg.getString("receiver") == clusterProvider.localNode().name) {
                    handleServiceMessage(msg)
                }
            }
        }

    }
}
