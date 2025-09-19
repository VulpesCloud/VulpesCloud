package de.vulpescloud.connector.velocity

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import de.vulpescloud.api.events.services.ServiceStateChangeEvent
import de.vulpescloud.api.serversoftware.SoftwareType
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.bridge.BridgeAPI
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

@Suppress("unused")
class VelocityServerRegistrationHandler(
    private val proxyServer: ProxyServer,
    bridgeAPI: BridgeAPI.BridgeFutureAPI,
) {

    init {
        for (server in servers()) {
            proxyServer.getServer(server).ifPresent { proxyServer.unregisterServer(it.serverInfo) }
        }

        bridgeAPI.getServicesAPI().getAllServices().get(5, TimeUnit.SECONDS).forEach {
            this.registerServer(
                "${it.task.name}-${it.orderedId}",
                "0.0.0.0",
                it.port,
            ) // TODO: Add field for hostname to Services
        }

        bridgeAPI.getEventAPI().subscribe<ServiceStateChangeEvent> { ev ->
            val event = ev.event
            if (
                event.newState == ServiceStates.RUNNING &&
                    event.service.task.software.type != SoftwareType.PROXY
            ) {
                registerServer(
                    "${event.service.task.name}-${event.service.orderedId}",
                    event.service.node,
                    event.service.port,
                )
            } else if (
                event.newState == ServiceStates.STOPPED &&
                    event.service.task.software.type != SoftwareType.PROXY
            ) {
                unregisterServer("${event.service.task.name}-${event.service.orderedId}")
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
}
