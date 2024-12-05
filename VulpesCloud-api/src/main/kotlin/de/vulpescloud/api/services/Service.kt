package de.vulpescloud.api.services

import de.vulpescloud.api.Named
import de.vulpescloud.api.tasks.Task
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

interface Service : Named {

    fun task(): Task

    fun orderedId(): Int

    fun id(): UUID

    fun hostname(): String

    fun port(): Int

    fun runningNode(): String

    fun state(): ServiceStates

    fun maxPlayers(): Int

    fun onlinePlayersCountAsync(): CompletableFuture<Int>

    // fun onlinePlayersAsync(): CompletableFuture<List<Player?>?>

    fun isEmpty(): Boolean {
        return this.onlinePlayersCount() == 0
    }

    fun onlinePlayersCount(): Int {
        return onlinePlayersCountAsync()[5, TimeUnit.SECONDS]
    }

//    fun onlinePlayers(): List<Player?>? {
//        return onlinePlayersAsync()[5, TimeUnit.SECONDS]
//    }

    override fun name(): String {
        return task().name() + "-" + orderedId()
    }

}