package de.vulpescloud.node.event.redis.cluster

import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.cluster.NodeLogEvent
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.redis.ChannelListener
import de.vulpescloud.node.utils.JsonUtils
import org.slf4j.LoggerFactory

class NodeLogEventTrigger(val eventManager: EventManager) :
    ChannelListener(RedisChannels.VULPESCLOUD_EVENT_CLUSTER_NodeLogEvent.name) {
    private val logger = LoggerFactory.getLogger(NodeLogEventTrigger::class.java)

    override fun onMessage(message: String) {
        val msg = JsonUtils.parsePubSubMessage(message)

        if (msg.getString("type") != "EVENT") {
            logger.warn("Received Message that isn't type EVENT. Raw message: &c$message")
            return
        }

        val event = msg.getJSONObject("eventData")

        eventManager.callLocal(
            NodeLogEvent(
                JsonUtils.getClusterNode(event.getJSONObject("node")),
                event.getString("level"),
                event.getString("log"),
                event.getString("formattedLog"),
            )
        )
    }
}
