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