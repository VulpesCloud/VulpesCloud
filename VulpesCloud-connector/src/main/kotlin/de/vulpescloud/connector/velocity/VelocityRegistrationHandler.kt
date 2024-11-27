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