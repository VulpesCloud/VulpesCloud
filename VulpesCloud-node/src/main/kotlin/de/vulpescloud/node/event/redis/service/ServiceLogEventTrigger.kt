package de.vulpescloud.node.event.redis.service

import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.service.ServiceLogEvent
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.redis.ChannelListener
import de.vulpescloud.node.utils.JsonUtils.getService
import de.vulpescloud.node.utils.JsonUtils.parsePubSubMessage
import org.slf4j.LoggerFactory

class ServiceLogEventTrigger(private val eventManager: EventManager) :
    ChannelListener(RedisChannels.VULPESCLOUD_EVENT_SERVICE_ServiceLogEvent.name) {

    private val logger = LoggerFactory.getLogger(ServiceLogEventTrigger::class.java)

    override fun onMessage(message: String) {
        val msg = parsePubSubMessage(message)

        if (msg.getString("type") != "EVENT") {
            logger.warn("Received Message that isn't type EVENT. Raw message: &c$message")
            return
        }

        val event = msg.getJSONObject("eventData")

        eventManager.callLocal(
            ServiceLogEvent(
                getService(event.getJSONObject("service")),
                event.getString("rawMessage"),
            )
        )
    }
}
