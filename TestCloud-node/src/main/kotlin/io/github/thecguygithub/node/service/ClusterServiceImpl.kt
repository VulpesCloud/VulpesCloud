package io.github.thecguygithub.node.service

import io.github.thecguygithub.api.JavaCloudAPI
import io.github.thecguygithub.api.players.ClusterPlayer
import io.github.thecguygithub.api.services.ClusterService
import io.github.thecguygithub.api.services.ClusterServiceStates
import io.github.thecguygithub.api.tasks.ClusterTask
import io.github.thecguygithub.node.Node
import java.util.*
import java.util.concurrent.CompletableFuture



open class ClusterServiceImpl(
    val task: ClusterTask,
    val orderedId: Int,
    val id: UUID,
    val port: Int,
    val hostname: String,
    val runningNode: String,
    var maxPlayers: Int = 0,  // Optional setter for maxPlayers
    var state: ClusterServiceStates = ClusterServiceStates.PREPARED  // Default value for state
) : ClusterService {

    override fun details(): String {
        return "id&8=&7$id&8, &7hostname&8=&7$hostname, &7port&8=&7$port&8, &7node&8=&7$runningNode"
    }

    override fun group(): ClusterTask {
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

        Node.instance?.getRC()?.sendMessage("SERVICE;$id;SHUTDOWN", "testcloud-service-actions")

    }

    override fun executeCommand(command: String?) {
        Node.instance?.getRC()?.sendMessage("SERVICE;$id;COMMAND;$command", "testcloud-service-actions")
    }

    override fun state(starting: ClusterServiceStates): ClusterServiceStates {
        return state
    }

    override fun update() {
        // TODO: Call head node and broadcast this to all nodes
    }

    override fun maxPlayers(): Int {
        return maxPlayers
    }

    override fun onlinePlayersCountAsync(): CompletableFuture<Int> {
        return CompletableFuture.completedFuture(onlinePlayers()?.size)
    }

    override fun onlinePlayersAsync(): CompletableFuture<List<ClusterPlayer?>?> {
        return CompletableFuture.completedFuture(
            JavaCloudAPI.getInstance().playerProvider().players()
                ?.filter { it?.currentServer()?.id() == id || it?.currentProxy()?.id() == id }
        )
    }
}
