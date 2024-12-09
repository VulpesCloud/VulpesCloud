package de.vulpescloud.connector.bukkit

import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.wrapper.redis.RedisJsonParser
import de.vulpescloud.wrapper.redis.RedisManager
import org.bukkit.Bukkit

class BukkitRedisListener {

    private val redis = BukkitConnector.instance.connector.wrapper.getRC()
    private val redisManager = redis?.let { RedisManager(it.getJedisPool()) }
    private val redisChannels = listOf(
        RedisChannelNames.VULPESCLOUD_SERVICE_ACTION.name,
        RedisChannelNames.VULPESCLOUD_SERVICE_EVENT.name
    )

    init {
        redisManager?.subscribe(redisChannels) { _, channel, msg ->
            when (channel) {
                RedisChannelNames.VULPESCLOUD_SERVICE_ACTION.name -> {
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