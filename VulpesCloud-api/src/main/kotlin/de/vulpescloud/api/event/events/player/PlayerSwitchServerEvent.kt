package de.vulpescloud.api.event.events.player

import de.vulpescloud.api.player.VulpesPlayer
import de.vulpescloud.api.services.Service

data class PlayerSwitchServerEvent(
    val player: VulpesPlayer,
    val newServer: Service,
    val oldServer: Service?
)
