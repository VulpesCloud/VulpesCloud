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

package de.vulpescloud.connector.bukkit

import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.wrapper.redis.RedisJsonParser
import de.vulpescloud.wrapper.redis.RedisManager
import org.bukkit.Bukkit

class BukkitRedisSubscribe {

    private val redis = BukkitConnector.instance.connector.wrapper.getRC()
    private val redisManager = redis?.let { RedisManager(it.getJedisPool()) }
    private val redisChannels = listOf(
        RedisPubSubChannels.VULPESCLOUD_SERVICE_ACTION.name,
        RedisPubSubChannels.VULPESCLOUD_SERVICE_EVENT.name
    )

    init {

        redisManager?.subscribe(redisChannels) { _, channel, msg ->
            when (channel) {
                RedisPubSubChannels.VULPESCLOUD_SERVICE_ACTION.name -> {
                    val message = msg?.let { RedisJsonParser.parseJson(it) }
                        ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                    val splitMSG = message!!.split(";")

                    if (splitMSG[1] == BukkitConnector.instance.connector.wrapper.service.name) {
                        if (splitMSG[0] == "SERVICE") {
                            if (splitMSG[2] == "ACTION") {
                                if (splitMSG[3] == "STOP") {
                                    BukkitConnector.instance.server.shutdown()
                                } else if (splitMSG[3] == "COMMAND") {
                                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), splitMSG[4])
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}