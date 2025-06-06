package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.jediswrapper.redis.ChannelListener
import de.vulpescloud.node.service.NodeCommunicationServiceHandler.handleServiceMessage
import de.vulpescloud.node.utils.JsonUtils.parsePubSubMessage
import org.slf4j.LoggerFactory

class NodeCommunicationChannelListener(private val clusterProvider: ClusterProvider) : ChannelListener("VULPESCLOUD_NODE_COMMUNICATION") {
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
