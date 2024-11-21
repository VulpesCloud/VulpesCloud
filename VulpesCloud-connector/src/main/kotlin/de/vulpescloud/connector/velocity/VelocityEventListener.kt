package de.vulpescloud.connector.velocity

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import de.vulpescloud.api.services.ClusterServiceFilter

class VelocityEventListener {

    private val proxyServer = VelocityConnector.instance.proxyServer

    @Subscribe(order = PostOrder.FIRST)
    fun playerChooseInitialServerEvent(event: PlayerChooseInitialServerEvent) {
        val fallbackServer = VelocityConnector.instance.serviceProvider.findByFilter(ClusterServiceFilter.FALLBACKS)

        if (fallbackServer?.isEmpty()!!) {
            println("FALLBACK SERVER IS NULL!")
            event.setInitialServer(null)
            return
        }

        //todo maybe send redis Message if there is no fallback
        println(fallbackServer[0].name())
        proxyServer.getServer(fallbackServer[0].name()).ifPresent { event.setInitialServer(it) }

    }

}