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

package de.vulpescloud.api.players

import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


abstract class PlayerProvider {
    abstract fun onlineAsync(uuid: UUID): CompletableFuture<Boolean>

    abstract fun onlineAsync(name: String): CompletableFuture<Boolean>

    abstract fun playersCountAsync(): CompletableFuture<Int?>

    abstract fun playersAsync(): CompletableFuture<List<Player?>?>

    abstract fun findAsync(uuid: UUID): CompletableFuture<Player?>

    abstract fun findAsync(name: String): CompletableFuture<Player?>

    @SneakyThrows
    fun getPlayerByName(name: String): Player? {
        return findAsync(name)[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun getPlayerByUUID(uuid: UUID): Player? {
        return findAsync(uuid)[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun isPlayerOnline(name: String): Boolean {
        return onlineAsync(name)[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun isPlayerOnline(uuid: UUID): Boolean {
        return onlineAsync(uuid)[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun players(): List<Player?>? {
        return playersAsync()[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun playersCount(): Int? {
        return playersCountAsync()[5, TimeUnit.SECONDS]
    }
}