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

package de.vulpescloud.connector.velocity

import com.velocitypowered.api.proxy.server.ServerInfo
import java.net.InetSocketAddress

object VelocityRegistrationHandler {

    private val proxyServer = VelocityConnector.instance.proxyServer

    init {
        for (server in servers()) {
            proxyServer.getServer(server).ifPresent { proxyServer.unregisterServer(it.serverInfo) }
        }
        proxyServer.registerServer(ServerInfo("lobby-1", InetSocketAddress("127.0.0.1", 30000)))

        servers().forEach { println(it) }
    }

    fun registerServer(name: String, address: String, port: Int) {
        // proxyServer.registerServer(ServerInfo(name, InetSocketAddress(address, port)))
        servers().forEach { println(it) }
    }

    fun unregisterServer(name: String) {
        proxyServer.getServer(name).ifPresent {
            proxyServer.unregisterServer(
                it.serverInfo
            )
        }
        servers().forEach { println(it) }
    }

    fun servers(): List<String> {
        return VelocityConnector.instance.proxyServer.allServers.stream().map { it.serverInfo.name }.toList()
    }

}