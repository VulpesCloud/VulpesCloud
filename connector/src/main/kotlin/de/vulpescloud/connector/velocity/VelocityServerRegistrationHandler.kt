package de.vulpescloud.connector.velocity

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import de.vulpescloud.api.events.services.ServiceStateChangeEvent
import de.vulpescloud.api.serversoftware.SoftwareType
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.bridge.BridgeAPI
import kotlinx.coroutines.Job
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

@Suppress("unused")
class VelocityServerRegistrationHandler(
    private val proxyServer: ProxyServer,
    bridgeAPI: BridgeAPI.BridgeFutureAPI,
) {

    private val serviceStateChangeEventJob: Job

    init {
        for (server in servers()) {
            proxyServer.getServer(server).ifPresent { proxyServer.unregisterServer(it.serverInfo) }
        }

        bridgeAPI.getServicesAPI().getAllServices().get(5, TimeUnit.SECONDS).forEach {
            if (it.task.software.type != SoftwareType.PROXY) {
                this.registerServer("${it.task.name}-${it.orderedId}", it.hostname, it.port)
            }
        }

        serviceStateChangeEventJob =
            bridgeAPI.getEventAPI().subscribe<ServiceStateChangeEvent> { ev ->
                val event = ev.event
                if (
                    event.newState == ServiceStates.RUNNING &&
                        event.service.task.software.type != SoftwareType.PROXY
                ) {
                    registerServer(
                        "${event.service.task.name}-${event.service.orderedId}",
                        event.service.hostname,
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

    fun shutdown() {
        serviceStateChangeEventJob.cancel()
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
