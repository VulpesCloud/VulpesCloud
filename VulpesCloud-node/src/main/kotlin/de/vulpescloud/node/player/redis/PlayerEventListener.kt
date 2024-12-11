package de.vulpescloud.node.player.redis

import de.vulpescloud.api.mysql.tables.PlayerTable
import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.node.Node
import de.vulpescloud.node.networking.redis.RedisJsonParser
import de.vulpescloud.node.networking.redis.RedisManager
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object PlayerEventListener {

    private val redisManager = Node.instance.getRC()?.let { RedisManager(it.getJedisPool()) }
    private val logger = LoggerFactory.getLogger(PlayerEventListener::class.java)

    init {
        redisManager?.subscribe(
            listOf(
                RedisChannelNames.VULPESCLOUD_PLAYER_EVENT.name
            )
        ) {_, _, msg ->
            val message = msg?.let { RedisJsonParser.parseJson(it) }
                ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

            val splitMSG = message!!.split(";")

            if (splitMSG[0] == "PLAYER") {
                if (splitMSG[1] == "EVENT") {
                    when (splitMSG[2]) {
                        "JOIN" -> {
                            logger.debug("Player ${splitMSG[3]}(${splitMSG[4]}) -> ${splitMSG[6]}")
                            transaction {
                                val players = PlayerTable.select(PlayerTable.uuid).toList()
                                val p = players.find { it[PlayerTable.uuid] == splitMSG[4]}
                                if (p == null) {
                                    PlayerTable.insert {
                                        it[name] = splitMSG[3]
                                        it[uuid] = splitMSG[4]
                                        it[ip] = "Not Implemented"
                                        it[firstLogin] = System.currentTimeMillis()
                                        it[lastLogin] = System.currentTimeMillis()
                                    }
                                } else {
                                    logger.warn("PlayerEventListener Line 45 Not Implemented, please report this in the VulpesCloud Discord")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}