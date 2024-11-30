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

package de.vulpescloud.bridge.networking.redis

import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.bridge.player.PlayerImpl
import de.vulpescloud.bridge.player.PlayerProvider
import de.vulpescloud.bridge.service.ServiceProvider
import de.vulpescloud.wrapper.Wrapper
import de.vulpescloud.wrapper.redis.RedisJsonParser
import de.vulpescloud.wrapper.redis.RedisManager
import java.util.*

class PlayerEvents {

    private val redis = Wrapper.instance.getRC()
    private val redisManager = redis?.let { RedisManager(it.getJedisPool()) }
    private val redisChannels = listOf(
        RedisPubSubChannels.VULPESCLOUD_PLAYER_EVENT.name
    )

    init {
        redisManager?.subscribe(redisChannels) { _,  channel, msg ->
            if (channel == RedisPubSubChannels.VULPESCLOUD_PLAYER_EVENT.name) {
                val message = msg?.let { RedisJsonParser.parseJson(it) }
                    ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                val splitMSG = message!!.split(";")
                if (splitMSG[0] == "PLAYER") {
                    if (splitMSG[1] == "EVENT") {
                        if (splitMSG[2] == "JOIN") {
                            if (splitMSG[5] == "PROXY") {
                                val player = PlayerImpl(splitMSG[3], UUID.fromString(splitMSG[4]))
                                val serviceProv = ServiceProvider()
                                serviceProv.getAllServiceFromRedis()
                                val proxy = serviceProv.findByName(splitMSG[6])
                                if (proxy == null) {
                                    println("WARN: Tried to register a player but the Service does not exist!")
                                } else {
                                    player.setProxy(proxy)
                                    PlayerProvider().addPlayerToMap(player)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
}