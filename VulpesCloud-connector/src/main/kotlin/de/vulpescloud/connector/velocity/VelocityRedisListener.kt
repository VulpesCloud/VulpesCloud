package de.vulpescloud.connector.velocity

import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.wrapper.Wrapper
import de.vulpescloud.wrapper.redis.RedisJsonParser
import de.vulpescloud.wrapper.redis.RedisManager

class VelocityRedisListener {
    private val redis = VelocityConnector.instance.wrapper.getRC()
    private val redisManager = redis?.let { RedisManager(it.getJedisPool()) }
    private val redisChannels = listOf(
        RedisChannelNames.VULPESCLOUD_SERVICE_ACTION.name,
        RedisChannelNames.VULPESCLOUD_SERVICE_EVENT.name,
        RedisChannelNames.VULPESCLOUD_SERVICE_REGISTER.name,
        RedisChannelNames.VULPESCLOUD_SERVICE_UNREGISTER.name,
        "debug_services"
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
                RedisChannelNames.VULPESCLOUD_SERVICE_REGISTER.name -> {
                    val message = msg?.let { RedisJsonParser.parseJson(it) }
                        ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                    val splitMSG = message!!.split(";")
                    // SERVICE;<service name>;REGISTER;ADDRESS;<service address>;PORT;<service port>
                    if (splitMSG[0] == "SERVICE") {
                        if (splitMSG[2] == "REGISTER") {
                            if (splitMSG[3] == "ADDRESS") {
                                if (splitMSG[5] == "PORT") {
                                    VelocityRegistrationHandler.registerServer(
                                        splitMSG[1],
                                        splitMSG[4],
                                        splitMSG[6].toInt()
                                    )
                                }
                            }
                        }
                    }
                }
                RedisChannelNames.VULPESCLOUD_SERVICE_UNREGISTER.name -> {
                    val message = msg?.let { RedisJsonParser.parseJson(it) }
                        ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                    val splitMSG = message!!.split(";")
                    // SERVICE;<service name>;UNREGISTER
                    if (splitMSG[0] == "SERVICE") {
                        if (splitMSG[2] == "UNREGISTER") {
                            VelocityRegistrationHandler.unregisterServer(splitMSG[1])
                        }
                    }
                }
                "debug_services" -> {
                    Wrapper.instance.getRC()?.sendMessage("Returning servers!", "debug_return")
                    VelocityConnector.instance.proxyServer.allServers.forEach {
                        Wrapper.instance.getRC()?.sendMessage(it.serverInfo.name, "debug_return")
                    }
                }
            }
        }
    }
}
