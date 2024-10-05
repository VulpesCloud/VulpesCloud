package io.github.thecguygithub.node.event

import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.networking.RedisJsonParser
import io.github.thecguygithub.node.networking.RedisManager

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

                Logger().info(splitMsg?.get(0).toString())

            }
        }

    }

}