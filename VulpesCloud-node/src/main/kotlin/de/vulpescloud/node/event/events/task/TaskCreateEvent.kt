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

package de.vulpescloud.node.event.events.task

import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.node.Node
import de.vulpescloud.node.event.events.Event
import de.vulpescloud.node.networking.redis.RedisJsonParser
import de.vulpescloud.node.task.TaskFactory
import org.json.JSONObject

object TaskCreateEvent : Event() {

    init {
        redisManager?.subscribe(listOf(RedisPubSubChannels.VULPESCLOUD_TASK_CREATE.name)) { _, _, msg ->
            val message = msg?.let { RedisJsonParser.parseJson(it) }
                ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

            val splitMSG = message!!.split(";")

            if (splitMSG[0] == "TASK") {
                if (splitMSG[1] == "CREATE") {
                    if (splitMSG[2] == "DATA") {
                        val json = try {
                            JSONObject(splitMSG[3])
                        } catch (e: Exception) {
                            logger.error("Error while parsing Task JSON in TaskCreateEvent! Exception: $e")
                            null
                        }
                        if (json != null) {
                            TaskFactory.createNewTask(json)
                        }
                    }
                }
            }
        }
    }

}