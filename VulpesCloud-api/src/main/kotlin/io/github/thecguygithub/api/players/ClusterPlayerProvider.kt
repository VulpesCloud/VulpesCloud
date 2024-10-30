package io.github.thecguygithub.api.players

import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


abstract class ClusterPlayerProvider {
    abstract fun onlineAsync(uuid: UUID?): CompletableFuture<Boolean>

    abstract fun onlineAsync(name: String?): CompletableFuture<Boolean>

    abstract fun playersCountAsync(): CompletableFuture<Int?>

    abstract fun playersAsync(): CompletableFuture<List<ClusterPlayer?>?>

    abstract fun findAsync(uuid: UUID?): CompletableFuture<ClusterPlayer?>

    abstract fun findAsync(name: String?): CompletableFuture<ClusterPlayer?>

    @SneakyThrows
    fun find(name: String?): ClusterPlayer? {
        return findAsync(name)[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun find(uuid: UUID?): ClusterPlayer? {
        return findAsync(uuid)[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun online(name: String?): Boolean {
        return onlineAsync(name)[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun online(uuid: UUID?): Boolean {
        return onlineAsync(uuid)[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun players(): List<ClusterPlayer?>? {
        return playersAsync()[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun playersCount(): Int? {
        return playersCountAsync()[5, TimeUnit.SECONDS]
    }
}