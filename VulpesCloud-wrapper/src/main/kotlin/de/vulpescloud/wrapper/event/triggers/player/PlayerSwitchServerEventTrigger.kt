package de.vulpescloud.wrapper.event.triggers.player

import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.player.PlayerSwitchServerEvent
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.bridge.JsonUtils.getPlayer
import de.vulpescloud.bridge.JsonUtils.getService
import de.vulpescloud.bridge.JsonUtils.parsePubSubMessage
import de.vulpescloud.jediswrapper.redis.ChannelListener
import org.slf4j.LoggerFactory

class PlayerSwitchServerEventTrigger(private val eventManager: EventManager) :
    ChannelListener(RedisChannels.VULPESCLOUD_EVENT_PLAYER_PlayerSwitchServerEvent.name) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun onMessage(message: String) {
        val msg = parsePubSubMessage(message)

        if (msg.getString("type") != "EVENT") {
            logger.warn("Received Message that isn't type EVENT. Raw message: &c$message")
            return
        }

        val event = msg.getJSONObject("eventData")

        eventManager.callLocal(
            PlayerSwitchServerEvent(
                getPlayer(event.getJSONObject("player")),
                getService(event.getJSONObject("oldService")),
                getService(event.getJSONObject("newService")),
            )
        )
    }
}
