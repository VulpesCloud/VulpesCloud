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
import de.vulpescloud.api.players.PlayerProvider
import de.vulpescloud.bridge.networking.redis.PlayerEvents
import java.util.*
import java.util.concurrent.CompletableFuture

open class PlayerProvider : PlayerProvider() {
    private val players: MutableMap<UUID, Player> = mutableMapOf()

    override fun onlineAsync(uuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(this.players.containsKey(uuid))
    }

    override fun onlineAsync(name: String): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(this.players.values.stream().anyMatch { it.name().equals(name, true) })
    }

    override fun playersCountAsync(): CompletableFuture<Int?> {
        return CompletableFuture.completedFuture(this.players.size)
    }

    override fun playersAsync(): CompletableFuture<List<Player?>?> {
        return CompletableFuture.completedFuture(this.players.values.stream().toList())
    }

    override fun findAsync(uuid: UUID): CompletableFuture<Player?> {
        return CompletableFuture.completedFuture(this.players[uuid])
    }

    override fun findAsync(name: String): CompletableFuture<Player?> {
        return CompletableFuture.completedFuture(this.players.values.stream().filter { it.name().equals(name, true) }.findFirst().orElse(null))
    }

    fun initializePlayerProvider() {
        PlayerEvents()
    }

    fun loadPlayerDataFromRedis() {
        //todo Add what the fun name says :D
    }

    fun addPlayerToMap(player: Player) {
        this.players[player.uniqueId()] = player
    }
}