package de.vulpescloud.node.event.events.service

import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.node.Node
import de.vulpescloud.node.event.events.Event
import de.vulpescloud.node.networking.redis.RedisJsonParser
import de.vulpescloud.node.service.Service

object ServiceLoggingEvent : Event() {

    private val channels = listOf(
        RedisPubSubChannels.VULPESCLOUD_SERVICE_EVENT.name
    )

    init {
        this.redisManager?.subscribe(channels) { _, channel, msg ->
            if (channel == RedisPubSubChannels.VULPESCLOUD_SERVICE_EVENT.name) {
                val message = msg?.let { RedisJsonParser.parseJson(it) }
                    ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                val splitMsg = message?.split("\uD835\uDF06")

                if (splitMsg!![0] == "SERVICE") {
                    if (splitMsg[2] == "EVENT") {
                        if (splitMsg[3] == "LOG") {
                            val service = Node.serviceProvider.findByName(splitMsg[1]) as Service
                            if (service.logging) {
                                logger.info("&8[ &m{} &8] &b{}", service.name(), splitMsg[4])
                            }
                        }
                    }
                }
            }
        }
    }

}