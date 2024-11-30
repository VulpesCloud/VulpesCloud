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

package de.vulpescloud.node.event.events

import de.vulpescloud.node.Node
import de.vulpescloud.node.networking.redis.RedisJsonParser
import de.vulpescloud.node.networking.redis.RedisManager
import de.vulpescloud.node.task.TaskFactory
import org.slf4j.LoggerFactory

object TaskUpdateEvent {

    private val redisManager = Node.instance!!.getRC()?.let { RedisManager(it.getJedisPool()) }
    private val logger = LoggerFactory.getLogger(TaskUpdateEvent::class.java)

    /**
    *   This Event will be triggered when a Node changes it State
    *   FORMATS:
    *       -> TASK;CREATE;<task info in Json>
    *       -> TASK;DELETE;<task name>
    *   CHANNEL: vulpescloud-event-task-update
    */
    init {
        redisManager?.subscribe(listOf("vulpescloud-event-task-update")) { _, channel, msg ->
            if (channel == "vulpescloud-event-task-update") {
                val message = msg?.let { RedisJsonParser.parseJson(it) }
                    ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                val splitMSG = message!!.split(";")

                if (splitMSG[0] == "TASK") {
                    if (splitMSG[1] == "CREATE") {
                        TaskFactory.createNewTask(RedisJsonParser.parseJson(splitMSG[2]))
                    } else if (splitMSG[1] == "DELETE") {
                        logger.warn("Please notify TheCGuy that this is not yet implemented!")
                    }
                }
            }
        }
    }
}