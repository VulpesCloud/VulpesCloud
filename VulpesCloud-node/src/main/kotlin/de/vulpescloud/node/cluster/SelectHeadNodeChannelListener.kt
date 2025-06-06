package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.redis.ChannelListener
import de.vulpescloud.node.utils.JsonUtils.parsePubSubMessage

class SelectHeadNodeChannelListener(
    clusterProvider: ClusterProvider,
) : ChannelListener(RedisChannels.VULPESCLOUD_CLUSTER_SelectNewHeadNode.name) {

    private val clusterProvider = clusterProvider as ClusterProviderImpl

    override fun onMessage(message: String) {
        val msg = parsePubSubMessage(message)

        val newHeadNodeName = msg.getString("newHeadNodeName")

        if (clusterProvider.localNode().name == newHeadNodeName) {
            clusterProvider.switchToHeadNode()
        }
    }
}
