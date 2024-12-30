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
                    val message = RedisJsonParser.convert(msg!!)

                    if (message.getString("service") == Wrapper.instance.service.name) {
                        when (message.getString("action")) {
                            "STOP" -> {
                                VelocityConnector.instance.proxyServer.shutdown()
                            }
                            "COMMAND" -> {
                                VelocityConnector.instance.proxyServer.commandManager.executeAsync(
                                    VelocityConnector.instance.proxyServer.consoleCommandSource,
                                    message.getString("parameter")
                                )
                            }
                        }
                    }
                }
                RedisChannelNames.VULPESCLOUD_SERVICE_REGISTER.name -> {
                    val message = RedisJsonParser.convert(msg!!)

                    VelocityRegistrationHandler.registerServer(
                        message.getString("serviceName"),
                        message.getString("address"),
                        message.getInt("port")
                    )
                }
                RedisChannelNames.VULPESCLOUD_SERVICE_UNREGISTER.name -> {
                    val message = RedisJsonParser.convert(msg!!)

                    VelocityRegistrationHandler.unregisterServer(
                        message.getString("serviceName")
                    )
                }
            }
        }
    }
}
