package de.vulpescloud.node.redis

import de.vulpescloud.jediswrapper.redis.RedisController

fun RedisController.hashExists(hash: String): Boolean {
    return this.getAllHashValues(hash)?.isNotEmpty() ?: false
}
