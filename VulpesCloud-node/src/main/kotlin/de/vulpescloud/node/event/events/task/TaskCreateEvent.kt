package de.vulpescloud.node.event.events.task

import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.node.Node
import de.vulpescloud.node.event.events.Event
import de.vulpescloud.node.networking.redis.RedisJsonParser
import de.vulpescloud.node.task.TaskFactory
import org.json.JSONObject

object TaskCreateEvent : Event() {

    init {
        redisManager?.subscribe(listOf(RedisPubSubChannels.VULPESCLOUD_CREATE_TASK.name)) { _, _, msg ->
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