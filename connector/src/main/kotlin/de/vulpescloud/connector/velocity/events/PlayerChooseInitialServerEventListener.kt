package de.vulpescloud.connector.velocity.events

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.bridge.BridgeAPI
import java.util.concurrent.TimeUnit

class PlayerChooseInitialServerEventListener(
    private val bridgeAPI: BridgeAPI.BridgeFutureAPI,
    private val proxyServer: ProxyServer,
) {

    @Subscribe(order = PostOrder.FIRST)
    fun onPlayerChooseInitialServerEvent(event: PlayerChooseInitialServerEvent) {
        val services =
            bridgeAPI
                .getServicesAPI()
                .getAllServices()
                .get(5, TimeUnit.SECONDS)
                .filter {
                    it.task.software.type != de.vulpescloud.api.serversoftware.SoftwareType.PROXY &&
                        it.task.fallback
                }
                .sortedBy { it.playerCount }
        if (services.isEmpty()) {
            return
        }

        proxyServer.getServer("${services[0].task.name}-${services[0].orderedId}").ifPresent {
            event.setInitialServer(it)
        }
    }
}
