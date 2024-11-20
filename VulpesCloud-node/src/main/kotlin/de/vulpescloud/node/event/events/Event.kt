package de.vulpescloud.node.event.events

import de.vulpescloud.node.Node
import de.vulpescloud.node.networking.redis.RedisManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class Event {

    val redisManager = Node.instance!!.getRC()?.let { RedisManager(it.getJedisPool()) }
    val logger: Logger = LoggerFactory.getLogger(NodeStateChangeEvent::class.java)

}