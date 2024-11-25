package de.vulpescloud.node.player

import de.vulpescloud.api.network.redis.RedisHashNames
import de.vulpescloud.api.players.Player
import de.vulpescloud.api.players.PlayerProvider
import de.vulpescloud.node.Node
import de.vulpescloud.node.event.events.player.PlayerEvents
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CompletableFuture

class PlayerProvider : PlayerProvider() {
    private val players: Map<UUID, Player> = emptyMap()
    private val logger = LoggerFactory.getLogger(PlayerProvider::class.java)

    init {
        PlayerEvents()
    }

    override fun onlineAsync(uuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(this.players.containsKey(uuid))
    }

    override fun onlineAsync(name: String): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(this.players.values.stream().anyMatch { it.name().equals(name, true) })
    }

    override fun playersCountAsync(): CompletableFuture<Int?> {
        return CompletableFuture.completedFuture(this.players.size)
    }

    override fun playersAsync(): CompletableFuture<List<Player?>?> {
        return CompletableFuture.completedFuture(this.players.values.stream().toList())
    }

    override fun findAsync(uuid: UUID): CompletableFuture<Player?> {
        return CompletableFuture.completedFuture(this.players[uuid])
    }

    override fun findAsync(name: String): CompletableFuture<Player?> {
        return CompletableFuture.completedFuture(this.players.values.stream().filter { it.name().equals(name, true) }.findFirst().orElse(null))
    }

    fun overridePlayersFromRedis() {
        val players: MutableMap<UUID, Player> = mutableMapOf()
        Node.instance!!.getRC()?.getAllHashValues(RedisHashNames.VULPESCLOUD_PLAYERS_ONLINE.name)!!.forEach {
            val json = try {
                JSONObject(it)
            } catch (e: Exception) {
                logger.error("Error whilst parsing Json: {}", e.message)
                return
            }
            val uuid = Node.instance!!.getRC()?.getHashFieldNameByValue(RedisHashNames.VULPESCLOUD_PLAYERS_ONLINE.name, it)
            val player = PlayerImpl(json.getString("name"), UUID.fromString(uuid))
            players[UUID.fromString(uuid)] = player
            // todo Add the Proxy and Server!
        }
    }
}