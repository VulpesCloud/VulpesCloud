package de.vulpescloud.bridge.event

import de.vulpescloud.api.event.events.player.PlayerJoinEvent
import de.vulpescloud.api.event.events.player.PlayerKickEvent
import de.vulpescloud.api.event.events.player.PlayerLeaveEvent
import de.vulpescloud.api.event.events.service.ServiceDeleteEvent
import de.vulpescloud.api.event.events.service.ServicePrepareEvent
import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.bridge.json.PlayerSerializer.playerFromJson
import de.vulpescloud.bridge.json.ServiceSerializer.serviceFromJson
import de.vulpescloud.wrapper.Wrapper
import de.vulpescloud.wrapper.redis.RedisJsonParser
import de.vulpescloud.wrapper.redis.RedisManager

class RedisEventListener {
    private val redisManager = RedisManager(Wrapper.instance.getRC()!!.getJedisPool())

    init {
        redisManager.subscribe(listOf(RedisChannelNames.VULPESCLOUD_EVENTS.name)) { _, _, message ->
            val json = RedisJsonParser.convert(message!!)

            when (json.getString("eventName")) {
                "ServiceDeleteEvent" -> {
                    EventManagerImpl.call(ServiceDeleteEvent(serviceFromJson(json.getJSONObject("eventData").getJSONObject("service"))))
                }
                "ServicePrepareEvent" -> {
                    EventManagerImpl.call(ServicePrepareEvent(serviceFromJson(json.getJSONObject("eventData").getJSONObject("service"))))
                }
                "PlayerJoinEvent" -> {
                    EventManagerImpl.call(PlayerJoinEvent(playerFromJson(json.getJSONObject("eventData").getJSONObject("player"))))
                }
                "PlayerLeaveEvent" -> {
                    EventManagerImpl.call(PlayerLeaveEvent(playerFromJson(json.getJSONObject("eventData").getJSONObject("player"))))
                }
                "PlayerKickEvent" -> {
                    EventManagerImpl.call(PlayerKickEvent(playerFromJson(json.getJSONObject("eventData").getJSONObject("player")), json.getJSONObject("eventData").getString("reason")))
                }
            }
        }
    }

}