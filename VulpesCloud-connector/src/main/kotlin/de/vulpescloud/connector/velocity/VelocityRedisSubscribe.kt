/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.connector.velocity

import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.wrapper.redis.RedisManager
import de.vulpescloud.wrapper.redis.RedisJsonParser

class VelocityRedisSubscribe {

    private val redis = VelocityConnector.instance.wrapper.getRC()
    private val redisManager = redis?.let { RedisManager(it.getJedisPool()) }
    private val redisChannels = listOf(
        RedisPubSubChannels.VULPESCLOUD_SERVICE_ACTION.name,
        RedisPubSubChannels.VULPESCLOUD_SERVICE_EVENT.name,
        RedisPubSubChannels.VULPESCLOUD_SERVICE_REGISTER.name,
        RedisPubSubChannels.VULPESCLOUD_SERVICE_UNREGISTER.name
    )

    init {

        redisManager?.subscribe(redisChannels) { _,  channel, msg ->
            when (channel) {
                RedisPubSubChannels.VULPESCLOUD_SERVICE_ACTION.name -> {
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
                RedisPubSubChannels.VULPESCLOUD_SERVICE_REGISTER.name -> {
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