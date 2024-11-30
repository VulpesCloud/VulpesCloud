/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.api.services

import de.vulpescloud.api.Detail
import de.vulpescloud.api.Named
import de.vulpescloud.api.players.Player
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

    fun onlinePlayersAsync(): CompletableFuture<List<Player?>?>

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
    fun onlinePlayers(): List<Player?>? {
        return onlinePlayersAsync()[5, TimeUnit.SECONDS]
    }

    /**
     * Returns the name of the service
     */
    override fun name(): String {
        return task().name() + "-" + orderedId()
    }

}