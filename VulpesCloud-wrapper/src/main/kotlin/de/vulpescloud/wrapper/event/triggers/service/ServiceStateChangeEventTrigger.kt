package de.vulpescloud.wrapper.event.triggers.service

import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.service.ServiceStateChangeEvent
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.bridge.JsonUtils.getService
import de.vulpescloud.bridge.JsonUtils.parsePubSubMessage
import de.vulpescloud.jediswrapper.redis.ChannelListener
import org.slf4j.LoggerFactory

class ServiceStateChangeEventTrigger(
    private val eventManager: EventManager,
) : ChannelListener(RedisChannels.VULPESCLOUD_EVENT_SERVICE_ServiceStateChangeEvent.name) {

    private val logger = LoggerFactory.getLogger(ServiceStateChangeEventTrigger::class.java)

    override fun onMessage(message: String) {
        val msg = parsePubSubMessage(message)

        if (msg.getString("type") != "EVENT") {
            logger.warn("Received Message that isn't type EVENT. Raw message: &c$message")
            return
        }

        val event = msg.getJSONObject("eventData")

        eventManager.callLocal(
            ServiceStateChangeEvent(
                getService(event.getJSONObject("service")),
                ServiceStates.valueOf(event.getString("oldState")),
                ServiceStates.valueOf(event.getString("newState"))
            )
        )
    }
}
