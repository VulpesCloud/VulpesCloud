package de.vulpescloud.node.event

import de.vulpescloud.node.Node
import de.vulpescloud.node.logging.Logger
import de.vulpescloud.node.networking.redis.RedisJsonParser
import de.vulpescloud.node.networking.redis.RedisManager

object NodeEventListener {

    private var channels = mutableListOf("testcloud-events-nodes", "testcloud-events-nodes-status")
    private val redisManger = Node.instance?.getRC()?.let { RedisManager(it.getJedisPool()) }

    init {
        val redis = Node.instance?.getRC()

        redisManger?.subscribe(channels) { _, channel, msg ->
            if (channel == "testcloud-events-nodes-status") {
                val message = msg?.let { RedisJsonParser.parseJson(it) }
                    ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                val splitMsg = message?.split(";")

                if (splitMsg!![1] == "NODE") {
                    if (splitMsg[3] == "STATUS") {
                        Logger().info("The Node ${splitMsg[2]} is ${splitMsg[4]}")
                    }
                }
            }
        }
    }
}