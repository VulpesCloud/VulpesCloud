package de.vulpescloud.connector.velocity

import build.buf.gen.vulpescloud.events.v1.ServiceStateChangedEvent
import build.buf.gen.vulpescloud.node.v1.SoftwareType
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.bridge.BridgeAPI
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

@Suppress("unused")
class VelocityServerRegistrationHandler(
    private val proxyServer: ProxyServer,
    bridgeAPI: BridgeAPI.BridgeFutureAPI,
) {
    private val logger = LoggerFactory.getLogger(VelocityServerRegistrationHandler::class.java)

    init {
        for (server in servers()) {
            proxyServer.getServer(server).ifPresent { proxyServer.unregisterServer(it.serverInfo) }
        }

        bridgeAPI.getServicesAPI().getAllServices().get(5, TimeUnit.SECONDS).forEach {
            if (it.task.software.type != de.vulpescloud.api.serversoftware.SoftwareType.PROXY) {
                this.registerServer("${it.task.name}-${it.orderedId}", it.hostname, it.port)
            }
        }

        bridgeAPI.getEventAPI().subscribe<ServiceStateChangedEvent> { event ->
            if (
                event.newState == ServiceStates.RUNNING.toServiceState() &&
                    event.service.task.serverSoftware.type != SoftwareType.SOFTWARE_TYPE_PROXY
            ) {
                registerServer(
                    "${event.service.task.name}-${event.service.orderedId}",
                    event.service.hostname,
                    event.service.port,
                )
            } else if (
                event.newState == ServiceStates.STOPPED.toServiceState() &&
                    event.service.task.serverSoftware.type != SoftwareType.SOFTWARE_TYPE_PROXY
            ) {
                unregisterServer("${event.service.task.name}-${event.service.orderedId}")
            }
        }
    }

    private fun registerServer(name: String, address: String, port: Int) {
        logger.info("Registering server $name at $address:$port")
        proxyServer.registerServer(ServerInfo(name, InetSocketAddress(address, port)))
    }

    private fun unregisterServer(name: String) {
        logger.info("Unregistering server $name")
        proxyServer.getServer(name).ifPresent { proxyServer.unregisterServer(it.serverInfo) }
    }

    private fun servers(): List<String> {
        return proxyServer.allServers.stream().map { it.serverInfo.name }.toList()
    }
}
