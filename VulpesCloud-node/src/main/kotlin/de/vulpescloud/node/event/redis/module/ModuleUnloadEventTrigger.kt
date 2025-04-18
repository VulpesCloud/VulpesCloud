package de.vulpescloud.node.event.redis.module

import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.modules.ModuleUnloadEvent
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.redis.ChannelListener
import de.vulpescloud.node.utils.JsonUtils.getClusterNode
import de.vulpescloud.node.utils.JsonUtils.getModuleInfo
import de.vulpescloud.node.utils.JsonUtils.parsePubSubMessage
import org.slf4j.LoggerFactory

class ModuleUnloadEventTrigger(private val eventManager: EventManager) :
    ChannelListener(RedisChannels.VULPESCLOUD_EVENT_MODULE_ModuleUnloadEvent.name) {

    private val logger = LoggerFactory.getLogger(ModuleUnloadEventTrigger::class.java)

    override fun onMessage(message: String) {
        val msg = parsePubSubMessage(message)

        if (msg.getString("type") != "EVENT") {
            logger.warn("Received Message that isn't type EVENT. Raw message: &c$message")
            return
        }

        val event = msg.getJSONObject("eventData")

        eventManager.callLocal(
            ModuleUnloadEvent(
                getModuleInfo(event.getJSONObject("module")),
                getClusterNode(event.getJSONObject("node")),
            )
        )
    }
}
