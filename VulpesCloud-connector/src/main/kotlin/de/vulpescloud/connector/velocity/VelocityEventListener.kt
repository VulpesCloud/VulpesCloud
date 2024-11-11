package de.vulpescloud.connector.velocity

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent

class VelocityEventListener {

    val proxyServer = VelocityConnector.instance.proxyServer

    @Subscribe
    fun playerChooseInitialServerEvent(event: PlayerChooseInitialServerEvent) {
        // val fallbackServer = todo Implement getting all fallbacks
    }

}