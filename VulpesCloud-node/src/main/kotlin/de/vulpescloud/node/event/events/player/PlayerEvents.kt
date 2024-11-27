package de.vulpescloud.node.event.events.player

import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.node.Node
import de.vulpescloud.node.networking.redis.RedisJsonParser
import de.vulpescloud.node.networking.redis.RedisManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class PlayerEvents {

    private val redis = Node.instance!!.getRC()
    private val redisManager = redis?.let { RedisManager(it.getJedisPool()) }
    private val redisChannels = listOf(
        RedisPubSubChannels.VULPESCLOUD_PLAYER_EVENT.name
    )

    init {
        redisManager?.subscribe(redisChannels) { _,  channel, msg ->
            if (channel == RedisPubSubChannels.VULPESCLOUD_PLAYER_EVENT.name) {
                val message = msg?.let { RedisJsonParser.parseJson(it) }
                    ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                val splitMSG = message!!.split(";")
                if (splitMSG[0] == "PLAYER") {
                    if (splitMSG[1] == "EVENT") {
                        if (splitMSG[2] == "JOIN") {
                            if (splitMSG[5] == "PROXY") {
                                GlobalScope.launch {
                                    delay(1000)
                                    Node.playerProvider.overridePlayersFromRedis()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}