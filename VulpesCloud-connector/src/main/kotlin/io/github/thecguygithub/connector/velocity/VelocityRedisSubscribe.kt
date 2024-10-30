package io.github.thecguygithub.connector.velocity

import io.github.thecguygithub.node.networking.redis.RedisManager
import io.github.thecguygithub.wrapper.networking.redis.RedisJsonParser

class VelocityRedisSubscribe {

    private val redis = VelocityConnector.instance.wrapper.getRC()
    private val redisManager = redis?.let { RedisManager(it.getJedisPool()) }
    private val redisChannels = listOf("vulpescloud-action-service")
    /*vulpescloud-action-service
    *-> SERVICE;<service id/uuid>;ACTION;<action>;[<parameter>]
    *-> SERVICE;<service id/uuid>;ACTION;STOP
    *-> SERVICE;<service id/uuid>;ACTION;COMMAND;[<parameter>]
    */
    init {

        redisManager?.subscribe(redisChannels) { _,  channel, msg ->
            when (channel) {
                "vulpescloud-action-service" -> {
                    val message = msg?.let { RedisJsonParser.parseJson(it) }
                        ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                    val splitMSG = message!!.split(";")

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