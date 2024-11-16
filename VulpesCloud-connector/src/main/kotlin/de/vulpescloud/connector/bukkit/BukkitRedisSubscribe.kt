package de.vulpescloud.connector.bukkit

import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.wrapper.redis.RedisJsonParser
import de.vulpescloud.wrapper.redis.RedisManager
import org.bukkit.Bukkit

class BukkitRedisSubscribe {

    private val redis = BukkitConnector.instance.connector.wrapper.getRC()
    private val redisManager = redis?.let { RedisManager(it.getJedisPool()) }
    private val redisChannels = listOf(
        RedisPubSubChannels.VULPESCLOUD_ACTION_SERVICE.name,
        RedisPubSubChannels.VULPESCLOUD_EVENT_SERVICE.name
    )

    init {

        redisManager?.subscribe(redisChannels) { _, channel, msg ->
            when (channel) {
                RedisPubSubChannels.VULPESCLOUD_ACTION_SERVICE.name -> {
                    val message = msg?.let { RedisJsonParser.parseJson(it) }
                        ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                    val splitMSG = message!!.split(";")

                    if (splitMSG[1] == BukkitConnector.instance.connector.wrapper.service.name) {
                        if (splitMSG[0] == "SERVICE") {
                            if (splitMSG[2] == "ACTION") {
                                if (splitMSG[3] == "STOP") {
                                    BukkitConnector.instance.server.shutdown()
                                } else if (splitMSG[3] == "COMMAND") {
                                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), splitMSG[4])
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}