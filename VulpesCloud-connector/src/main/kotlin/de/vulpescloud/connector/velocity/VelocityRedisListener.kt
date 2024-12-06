package de.vulpescloud.connector.velocity

import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.wrapper.redis.RedisJsonParser
import de.vulpescloud.wrapper.redis.RedisManager

class VelocityRedisListener {
    private val redis = VelocityConnector.instance.wrapper.getRC()
    private val redisManager = redis?.let { RedisManager(it.getJedisPool()) }
    private val redisChannels = listOf(
        RedisChannelNames.VULPESCLOUD_SERVICE_ACTION.name,
        RedisChannelNames.VULPESCLOUD_SERVICE_EVENT.name
    )

    init {
        redisManager?.subscribe(redisChannels) { _,  channel, msg ->
            when (channel) {
                RedisChannelNames.VULPESCLOUD_SERVICE_ACTION.name -> {
                    val message = msg?.let { RedisJsonParser.parseJson(it) }
                        ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                    val splitMSG = message!!.split(";")

                    if (splitMSG[1] == VelocityConnector.instance.wrapper.service.name) {
                        if (splitMSG[0] == "SERVICE") {
                            if (splitMSG[2] == "ACTION") {
                                if (splitMSG[3] == "STOP") {
                                    VelocityConnector.instance.proxyServer.shutdown()
                                } else if (splitMSG[3] == "COMMAND") {
                                    VelocityConnector.instance.proxyServer.commandManager.executeAsync(
                                        VelocityConnector.instance.proxyServer.consoleCommandSource,
                                        splitMSG[4]
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
