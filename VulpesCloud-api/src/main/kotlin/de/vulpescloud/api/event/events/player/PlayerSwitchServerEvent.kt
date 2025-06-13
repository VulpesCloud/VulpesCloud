package de.vulpescloud.api.event.events.player

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.player.Player
import de.vulpescloud.api.service.ServiceInfo

data class PlayerSwitchServerEvent(
    val player: Player,
    val oldServiceInfo: ServiceInfo,
    val newServiceInfo: ServiceInfo,
) : Event
