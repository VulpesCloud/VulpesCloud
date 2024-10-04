package io.github.thecguygithub.api.services

import io.github.thecguygithub.api.Named
import io.github.thecguygithub.api.players.ClusterPlayer
import io.github.thecguygithub.api.tasks.ClusterTask
import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


interface ClusterService : Named {

    fun group(): ClusterTask

    fun orderedId(): Int

    fun id(): UUID?

    fun hostname(): String?

    fun port(): Int

    fun runningNode(): String?

    fun shutdown()

    fun executeCommand(command: String?)

    fun state(): ClusterServiceStates?

    fun logs(): List<String?>?

    fun update()

    fun maxPlayers(): Int

    fun onlinePlayersCountAsync(): CompletableFuture<Int>

    fun onlinePlayersAsync(): CompletableFuture<List<ClusterPlayer?>?>


    fun isEmpty(): Boolean {
        return this.onlinePlayersCount() == 0
    }

    @SneakyThrows
    fun onlinePlayersCount(): Int {
        return onlinePlayersCountAsync()[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun onlinePlayers(): List<ClusterPlayer?>? {
        return onlinePlayersAsync()[5, TimeUnit.SECONDS]
    }

    override fun name(): String {
        return group().name() + "-" + orderedId()
    }

}