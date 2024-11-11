package de.vulpescloud.connector.velocity

import de.vulpescloud.wrapper.redis.RedisManager
import de.vulpescloud.wrapper.redis.RedisJsonParser

class VelocityRedisSubscribe {

    private val redis = VelocityConnector.instance.wrapper.getRC()
    private val redisManager = redis?.let { RedisManager(it.getJedisPool()) }
    private val redisChannels = listOf("vulpescloud-action-service", "vulpescloud-event-service", "vulpescloud-register-service")
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
                "vulpescloud-register-service" -> {
                    val message = msg?.let { RedisJsonParser.parseJson(it) }
                        ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                    val splitMSG = message!!.split(";")
                    // SERVICE;<service name>;ADDRESS;<service address>;PORT;<service port>;REGISTER
                    if (splitMSG[0] == "SERVICE") {
                        if (splitMSG[2] == "ADDRESS") {
                            if (splitMSG[4] == "PORT") {
                                if (splitMSG[6] == "REGISTER") {
                                    VelocityRegistrationHandler.registerServer(
                                        splitMSG[1],
                                        splitMSG[3],
                                        splitMSG[5].toInt()
                                    )
                                }
                            }
                        }
                    }
                }
                "vulpescloud-unregister-service" -> {
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
            }
        }

    }

}