package de.vulpescloud.node.event.events

import de.vulpescloud.node.Node
import de.vulpescloud.node.logging.Logger
import de.vulpescloud.node.networking.redis.RedisJsonParser
import de.vulpescloud.node.networking.redis.RedisManager

object NodeStateChangeEvent {

    private val redisManager = Node.instance!!.getRC()?.let { RedisManager(it.getJedisPool()) }
    /*
    *   This Event will be triggered when a Node changes it State
    *   FORMAT: NODE;<node>;STATE;<state>
    *   CHANNEL: vulpescloud-event-node-state
    */

    init {
        redisManager?.subscribe(listOf("vulpescloud-event-node-state")) { _, channel, msg, ->
            if (channel == "vulpescloud-event-node-state") {
                val message = msg?.let { RedisJsonParser.parseJson(it) }
                ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                val splitMSG = message!!.split(";")

                if (splitMSG[0] == "NODE") {
                    if (splitMSG[2] == "STATE") {
                    Logger().info("Node ${splitMSG[1]} is marked ${splitMSG[3]}")
                    }
                }
            }
        }
    }
}