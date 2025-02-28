package de.vulpescloud.connector.bukkit

import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.wrapper.Wrapper
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
                    val message = RedisJsonParser.convert(msg!!)

                    if (message.getString("service") == Wrapper.instance.serviceName) {
                        when (message.getString("action")) {
                            "STOP" -> {
                                Bukkit.getServer().shutdown()
                            }
                            "COMMAND" -> {
                                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), message.getString("parameter"))
                            }
                        }
                    }
                }
            }
        }
    }
}