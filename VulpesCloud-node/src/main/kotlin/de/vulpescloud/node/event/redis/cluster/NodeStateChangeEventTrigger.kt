package de.vulpescloud.node.event.redis.cluster

import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.cluster.NodeStateChangeEvent
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.redis.ChannelListener
import de.vulpescloud.node.utils.JsonUtils.getClusterNode
import de.vulpescloud.node.utils.JsonUtils.parsePubSubMessage
import org.slf4j.LoggerFactory

class NodeStateChangeEventTrigger(
    private val eventManager: EventManager
) :
    ChannelListener(RedisChannels.VULPESCLOUD_EVENT_CLUSTER_NodeStateChangeEvent.name) {

    private val logger = LoggerFactory.getLogger(NodeStateChangeEventTrigger::class.java)

    override fun onMessage(message: String) {
        val msg = parsePubSubMessage(message)

        if (msg.getString("type") != "EVENT") {
            logger.warn("Received Message that isn't type EVENT. Raw message: &c$message")
            return
        }

        eventManager.callLocal(
            NodeStateChangeEvent(
                getClusterNode(msg.getJSONObject("node")),
                NodeStates.valueOf(msg.getString("oldState")),
                NodeStates.valueOf(msg.getString("newState"))
            )
        )
    }
}
