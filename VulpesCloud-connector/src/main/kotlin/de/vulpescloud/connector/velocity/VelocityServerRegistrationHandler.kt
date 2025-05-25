package de.vulpescloud.connector.velocity

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import de.vulpescloud.api.event.EventListener
import de.vulpescloud.api.event.events.service.ServiceStateChangeEvent
import de.vulpescloud.api.service.ServiceFilter
import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.bridge.VulpesBridge
import java.net.InetSocketAddress

@Suppress("unused")
class VelocityServerRegistrationHandler(private val proxyServer: ProxyServer) {

    init {
        for (server in servers()) {
            proxyServer.getServer(server).ifPresent { proxyServer.unregisterServer(it.serverInfo) }
        }

        VulpesBridge.getServiceProvider().getServicesByFilter(ServiceFilter.SERVERS).forEach {
            if (it.state == ServiceStates.ONLINE && it.task.version.type != VersionType.PROXY) {
                this.registerServer(it.name, it.runningNode.hostname, it.port)
            }
        }
    }

    private fun registerServer(name: String, address: String, port: Int) {
        proxyServer.registerServer(ServerInfo(name, InetSocketAddress(address, port)))
    }

    private fun unregisterServer(name: String) {
        proxyServer.getServer(name).ifPresent { proxyServer.unregisterServer(it.serverInfo) }
    }

    private fun servers(): List<String> {
        return proxyServer.allServers.stream().map { it.serverInfo.name }.toList()
    }

    @EventListener
    fun onServiceStateChangeEvent(event: ServiceStateChangeEvent) {
        if (event.newState == ServiceStates.ONLINE) {
            registerServer(event.service.name, event.service.runningNode.hostname, event.service.port)
        } else if (event.newState == ServiceStates.STOPPING) {
            unregisterServer(event.service.name)
        }
    }

}
