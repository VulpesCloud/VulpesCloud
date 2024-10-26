package io.github.thecguygithub.node.event.events

import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.networking.redis.RedisJsonParser
import io.github.thecguygithub.node.networking.redis.RedisManager
import io.github.thecguygithub.node.task.TaskFactory

object TaskUpdateEvent {

    private val redisManager = Node.instance!!.getRC()?.let { RedisManager(it.getJedisPool()) }
    /*
    *   This Event will be triggered when a Node changes it State
    *   FORMATS:
    *       -> TASK;CREATE;<task info in Json>
    *       -> TASK;DELETE;<task name>
    *   CHANNEL: vulpescloud-event-task-update
    */
    init {
        redisManager?.subscribe(listOf("vulpescloud-event-task-update")) { _, channel, msg, ->
            if (channel == "vulpescloud-event-task-update") {
                val message = msg?.let { RedisJsonParser.parseJson(it) }
                    ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                val splitMSG = message!!.split(";")

                if (splitMSG[0] == "TASK") {
                    if (splitMSG[1] == "CREATE") {
                        TaskFactory.createNewTask(RedisJsonParser.parseJson(splitMSG[2]))
                    } else if (splitMSG[1] == "DELETE") {
                        Logger.instance.warn("Please notify TheCGuy that this is not yet implemented!")
                    }
                }
            }
        }
    }
}