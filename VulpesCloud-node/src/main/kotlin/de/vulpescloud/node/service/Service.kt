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

    var logging = false

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