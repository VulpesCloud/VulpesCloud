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

import de.vulpescloud.api.Named
import de.vulpescloud.api.services.ClusterService
import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


interface Player : Named {

    fun uniqueId(): UUID

    fun currentProxyName(): String

    fun currentServerName(): String

    fun currentProxyAsync(): CompletableFuture<ClusterService?>

    fun currentServerAsync(): CompletableFuture<ClusterService?>

    fun sendTitle(title: String, subtitle: String?, fadeIn: Int, stay: Int, fadeOut: Int)

    fun sendTablist(header: String, footer: String)

    fun sendMessage(message: String)

    fun sendActionBar(message: String)

    fun connect(serviceId: String)

    @SneakyThrows
    fun currentProxy(): ClusterService? {
        return currentProxyAsync()[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun currentServer(): ClusterService? {
        return currentServerAsync()[5, TimeUnit.SECONDS]
    }

    fun details(): String {
        return "uniqueId&8=&7" + uniqueId() + "&8, &7current proxy&8=&7" + currentProxyName() + ", &7 current server&8=&7" + currentServerName()
    }

    fun sendTitle(title: String, subtitle: String?) {
        //default minecraft values
        this.sendTitle(title, subtitle, 10, 70, 20)
    }

    fun connect(service: ClusterService) {
        this.connect(service.name())
    }

}