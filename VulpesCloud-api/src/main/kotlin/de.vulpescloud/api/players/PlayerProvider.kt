package de.vulpescloud.api.players

import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


abstract class PlayerProvider {
    abstract fun onlineAsync(uuid: UUID): CompletableFuture<Boolean>

    abstract fun onlineAsync(name: String): CompletableFuture<Boolean>

    abstract fun playersCountAsync(): CompletableFuture<Int?>

    abstract fun playersAsync(): CompletableFuture<List<Player?>?>

    abstract fun findAsync(uuid: UUID): CompletableFuture<Player?>

    abstract fun findAsync(name: String): CompletableFuture<Player?>

    @SneakyThrows
    fun getPlayerByName(name: String): Player? {
        return findAsync(name)[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun getPlayerByUUID(uuid: UUID): Player? {
        return findAsync(uuid)[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun isPlayerOnline(name: String): Boolean {
        return onlineAsync(name)[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun isPlayerOnline(uuid: UUID): Boolean {
        return onlineAsync(uuid)[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun players(): List<Player?>? {
        return playersAsync()[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun playersCount(): Int? {
        return playersCountAsync()[5, TimeUnit.SECONDS]
    }
}