package de.vulpescloud.connector.velocity

import com.velocitypowered.api.proxy.server.ServerInfo
import de.vulpescloud.api.services.ServiceFilters
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.bridge.service.ServiceProvider
import de.vulpescloud.wrapper.Wrapper
import java.net.InetSocketAddress

object VelocityRegistrationHandler {

    private val proxyServer = VelocityConnector.instance.proxyServer

    init {
        for (server in servers()) {
            proxyServer.getServer(server).ifPresent { proxyServer.unregisterServer(it.serverInfo) }
        }

        ServiceProvider.findServicesByFilter(ServiceFilters.SERVERS)?.forEach {
            if (it.state() == ServiceStates.ONLINE) {
                this.registerServer(it.name(), it.hostname(), it.port())
            }
        }

    }

    fun registerServer(name: String, address: String, port: Int) {
        Wrapper.instance.getRC()?.sendMessage("Registering -> $name, $address:$port", "test_return")
        proxyServer.registerServer(ServerInfo(name, InetSocketAddress(address, port)))
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