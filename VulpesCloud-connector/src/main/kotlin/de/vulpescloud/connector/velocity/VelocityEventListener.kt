package de.vulpescloud.connector.velocity

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import de.vulpescloud.api.services.ClusterServiceFilter

class VelocityEventListener {

    private val proxyServer = VelocityConnector.instance.proxyServer

    @Subscribe
    fun playerChooseInitialServerEvent(event: PlayerChooseInitialServerEvent) {
        val fallbackServer = VelocityConnector.instance.serviceProvider.findByFilter(ClusterServiceFilter.LOWEST_FALLBACK)

        if (fallbackServer?.isEmpty()!!) {
            event.setInitialServer(null)
            return
        }

        //todo maybe send redis Message if there is no fallback

        proxyServer.getServer(fallbackServer[0].name()).ifPresent { event.setInitialServer(it) }
    }

}