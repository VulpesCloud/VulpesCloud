package de.vulpescloud.node.services

import de.vulpescloud.api.player.VulpesPlayer
import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.json.ServiceSerializer.jsonFromService
import java.util.*
import java.util.concurrent.CompletableFuture

open class Service(
    open val task: Task,
    open val orderedId: Int,
    open val id: UUID,
    open val port: Int,
    open val hostname: String,
    open val runningNode: String,
    open var maxPlayers: Int = task.maxPlayers(),
    open var state: ServiceStates = ServiceStates.LOADING,
    open var logging: Boolean = false
) : Service {

    fun details(): String {
        return "id&8=&7$id&8, &7hostname&8=&7$hostname, &7port&8=&7$port&8, &7node&8=&7$runningNode&8, &7state&8=&7${state.name}&8, &7state&8=&7$state"
    }

    fun updateState(state: ServiceStates) {
        this.state = state
        Node.instance.getRC()?.setHashField(RedisHashNames.VULPESCLOUD_SERVICES.name, name(), jsonFromService(this).toString())
    }

    fun logging(): Boolean {
        return this.logging
    }
    override fun task(): Task {
        return task
    }

    override fun orderedId(): Int {
        return orderedId
    }

    override fun id(): UUID {
        return id
    }

    override fun hostname(): String {
        return hostname
    }

    override fun port(): Int {
        return port
    }

    override fun runningNode(): String {
        return runningNode
    }


    override fun state(): ServiceStates {
        return state
    }

    override fun maxPlayers(): Int {
        return maxPlayers
    }

    override fun onlinePlayersCountAsync(): CompletableFuture<Int> {
        return CompletableFuture.completedFuture(onlinePlayers()?.size)
    }

    override fun onlinePlayersAsync(): CompletableFuture<List<VulpesPlayer?>?> {
        return CompletableFuture.completedFuture(
            Node.instance.playerProvider.onlinePlayers().filter { it.currentServer()?.id() == id() || it.currentProxy()?.id() == id() }
        )
    }
}