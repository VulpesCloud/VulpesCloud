package de.vulpescloud.bridge.player

import de.vulpescloud.api.players.Player
import de.vulpescloud.api.players.PlayerProvider
import de.vulpescloud.bridge.networking.redis.PlayerEvents
import java.util.*
import java.util.concurrent.CompletableFuture

open class PlayerProvider : PlayerProvider() {
    private val players: MutableMap<UUID, Player> = mutableMapOf()

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

    fun initializePlayerProvider() {
        PlayerEvents()
    }

    fun loadPlayerDataFromRedis() {
        //todo Add what the fun name says :D
    }

    fun addPlayerToMap(player: Player) {
        this.players[player.uniqueId()] = player
    }
}