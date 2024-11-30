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

package de.vulpescloud.bridge.player

import de.vulpescloud.api.players.Player
import de.vulpescloud.api.services.ClusterService
import java.util.*
import java.util.concurrent.CompletableFuture

class PlayerImpl(
    val name: String,
    val uuid: UUID
) : Player {

    var currentServer: ClusterService? = null
    var currentProxy: ClusterService? = null

    override fun uniqueId(): UUID {
        return this.uuid
    }

    fun setProxy(service: ClusterService) {
        this.currentProxy = service
    }
    fun setServer(service: ClusterService) {
        this.currentServer = service
    }

    override fun currentProxyName(): String {
        val player = PlayerProvider().getPlayerByUUID(uniqueId())
        return if (PlayerProvider().isPlayerOnline(uniqueId())) {
            if (player == null) {
                throw IllegalStateException("The Player is null but is also online! ")
            } else {
                if (currentProxy == null) {
                    throw IllegalStateException("CurrentProxy is null but the Player is online!")
                } else {
                    currentProxy!!.name()
                }
            }
        } else {
            "<none>"
        }
    }

    override fun currentServerName(): String {
        val player = PlayerProvider().getPlayerByUUID(uniqueId())
        return if (PlayerProvider().isPlayerOnline(uniqueId())) {
            if (player == null) {
                throw IllegalStateException("The Player is null but is also online!")
            } else {
                if (currentServer == null) {
                    throw IllegalStateException("CurrentServer is null but the Player is online!")
                } else {
                    currentServer!!.name()
                }
            }
        } else {
            "<none>"
        }
    }

    override fun currentProxyAsync(): CompletableFuture<ClusterService?> {
        return if (currentProxy == null) {
            CompletableFuture.completedFuture(null)
        } else {
            CompletableFuture.completedFuture(currentProxy)
        }
    }

    override fun currentServerAsync(): CompletableFuture<ClusterService?> {
        return if (currentServer == null) {
            CompletableFuture.completedFuture(null)
        } else {
            CompletableFuture.completedFuture(currentServer)
        }
    }

    override fun sendTitle(title: String, subtitle: String?, fadeIn: Int, stay: Int, fadeOut: Int) {
        TODO("Not yet implemented")
    }

    override fun sendTablist(header: String, footer: String) {
        TODO("Not yet implemented")
    }

    override fun sendMessage(message: String) {
        TODO("Not yet implemented")
    }

    override fun sendActionBar(message: String) {
        TODO("Not yet implemented")
    }

    override fun connect(serviceId: String) {
        TODO("Not yet implemented")
    }

    override fun name(): String {
        return this.name
    }
}