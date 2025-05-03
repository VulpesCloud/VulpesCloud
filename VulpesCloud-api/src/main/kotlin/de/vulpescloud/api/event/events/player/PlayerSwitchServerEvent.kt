package de.vulpescloud.api.event.events.player

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.player.Player
import de.vulpescloud.api.service.Service

data class PlayerSwitchServerEvent(
    val player: Player,
    val oldService: Service,
    val newService: Service,
) : Event
