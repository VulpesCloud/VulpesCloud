package de.vulpescloud.node.event.redis.module

import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.modules.ModuleLoadEvent
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.redis.ChannelListener
import de.vulpescloud.node.utils.JsonUtils.parsePubSubMessage
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class ModuleLoadEventTrigger(private val eventManager: EventManager) :
    ChannelListener(RedisChannels.VULPESCLOUD_EVENT_MODULE_ModuleLoadEvent.name) {

    private val logger = LoggerFactory.getLogger(ModuleLoadEventTrigger::class.java)

    override fun onMessage(message: String) {
        val msg = parsePubSubMessage(message)

        if (msg.getString("type") != "EVENT") {
            logger.warn("Received Message that isn't type EVENT. Raw message: &c$message")
            return
        }

        val event = msg.getJSONObject("eventData")

        eventManager.callLocal(
            ModuleLoadEvent(
                Json.decodeFromString(event.getJSONObject("module").toString()),
                Json.decodeFromString(event.getJSONObject("node").toString()),
            )
        )
    }
}
