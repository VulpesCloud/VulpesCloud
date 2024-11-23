package de.vulpescloud.node.service

import de.vulpescloud.api.players.Player
import de.vulpescloud.api.services.ClusterService
import de.vulpescloud.api.services.ClusterServiceStates
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import java.util.*
import java.util.concurrent.CompletableFuture

open class Service(
    val task: Task,
    val orderedId: Int,
    val id: UUID,
    val port: Int,
    val hostname: String,
    val runningNode: String,
    var maxPlayers: Int = 0,  // Optional setter for maxPlayers
    var state: ClusterServiceStates = ClusterServiceStates.PREPARED,  // Default value for state
) : ClusterService {

    override fun details(): String {
        return "id&8=&7$id&8, &7hostname&8=&7$hostname, &7port&8=&7$port&8, &7node&8=&7$runningNode"
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

    override fun shutdown() {
        // todo Replace Redis message with new Message Builder
        Node.instance?.getRC()?.sendMessage("SERVICE;$id;ACTION;STOP", "vulpescloud-action-service")
    }

    override fun executeCommand(command: String) {
        // todo Replace Redis message with new Message Builder
        Node.instance?.getRC()?.sendMessage("SERVICE;$id;ACTION;COMMAND;$command", "vulpescloud-action-service")
    }

    override fun state(): ClusterServiceStates {
        return state
    }

    override fun update() {
        // Call head node and broadcast this to all nodes
    }

    override fun maxPlayers(): Int {
        return maxPlayers
    }

    override fun onlinePlayersCountAsync(): CompletableFuture<Int> {
        return CompletableFuture.completedFuture(onlinePlayers()?.size)
    }

    override fun onlinePlayersAsync(): CompletableFuture<List<Player?>?> {
        return CompletableFuture.completedFuture(
            null
//            JavaCloudAPI.getInstance().playerProvider().players()
//                ?.filter { it?.currentServer()?.id() == id || it?.currentProxy()?.id() == id }
        )
    }
}