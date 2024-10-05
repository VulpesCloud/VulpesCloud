package io.github.thecguygithub.node.networking

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub

class RedisManager(private val jedisPool: JedisPool) {
    fun subscribe(channels: List<String>, onMessage: (pattern: String?, channel: String?, msg: String?) -> Unit) {
        val jedisPubSub = object : JedisPubSub() {
            override fun onPMessage(pattern: String?, channel: String?, msg: String?) {
                onMessage(pattern, channel, msg)
            }
        }

        Thread {
            try {
                jedisPool.resource.use { jedis ->
                    jedis?.psubscribe(jedisPubSub, *channels.toTypedArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}