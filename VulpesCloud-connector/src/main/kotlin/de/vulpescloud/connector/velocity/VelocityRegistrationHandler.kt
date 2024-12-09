package de.vulpescloud.connector.velocity

import com.velocitypowered.api.proxy.server.ServerInfo
import de.vulpescloud.wrapper.Wrapper
import java.net.InetSocketAddress

object VelocityRegistrationHandler {

    private val proxyServer = VelocityConnector.instance.proxyServer

    init {
        servers().forEach { println(it) }
        for (server in servers()) {
            proxyServer.getServer(server).ifPresent { proxyServer.unregisterServer(it.serverInfo) }
        }

        servers().forEach { println(it) }
    }

    fun registerServer(name: String, address: String, port: Int) {
        Wrapper.instance.getRC()?.sendMessage("Registering -> $name, $address:$port", "test_return")
        proxyServer.registerServer(ServerInfo(name, InetSocketAddress(address, port)))
        proxyServer.allServers.forEach { println("srv ->" + it.serverInfo.name) }
    }

    fun unregisterServer(name: String) {
        proxyServer.getServer(name).ifPresent {
            proxyServer.unregisterServer(
                it.serverInfo
            )
        }
    }

    fun servers(): List<String> {
        return VelocityConnector.instance.proxyServer.allServers.stream().map { it.serverInfo.name }.toList()
    }

}