package de.vulpescloud.api.services

import de.vulpescloud.api.Detail
import de.vulpescloud.api.Named
import de.vulpescloud.api.players.ClusterPlayer
import de.vulpescloud.api.tasks.Task
import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * todo Add a good desc here xD
 */
interface ClusterService : Named, Detail {

    /**
     * Returns the Task that the service is created from
     * @see Task
     */
    fun task(): Task

    /**
     * Returns the orderedId as an Int that is mainly used to generate the Service name
     */
    fun orderedId(): Int

    /**
     * Returns the UUID of the Service
     */
    fun id(): UUID?

    /**
     * Returns the hostname of the Service
     */
    fun hostname(): String?

    /**
     * Returns the port that the service is using
     */
    fun port(): Int

    /**
     * Returns the node that the service is running on
     */
    fun runningNode(): String?

    /**
     * This fun just shuts the service down
     */
    fun shutdown()

    /**
     * This fun will send a Minecraft Command to the Service
     */
    fun executeCommand(command: String)

    /**
     * Returns the state that the Service currently is in
     */
    fun state(): ClusterServiceStates

    /**
     * This fun updates the Service redis hash field of the service
     */
    fun update()

    /**
     * Returns the maximum players that can join the service
     */
    fun maxPlayers(): Int

    fun onlinePlayersCountAsync(): CompletableFuture<Int>

    fun onlinePlayersAsync(): CompletableFuture<List<ClusterPlayer?>?>

    /**
     * Returns true if the service has no players on it
     */
    fun isEmpty(): Boolean {
        return this.onlinePlayersCount() == 0
    }

    /**
     * Returns the online Players from the service as an Int
     */
    @SneakyThrows
    fun onlinePlayersCount(): Int {
        return onlinePlayersCountAsync()[5, TimeUnit.SECONDS]
    }

    /**
     * Returns the online Players from the service as a List
     */
    @SneakyThrows
    fun onlinePlayers(): List<ClusterPlayer?>? {
        return onlinePlayersAsync()[5, TimeUnit.SECONDS]
    }

    /**
     * Returns the name of the service
     */
    override fun name(): String {
        return task().name() + "-" + orderedId()
    }

}