package de.vulpescloud.api.event.events.player

import de.vulpescloud.api.player.VulpesPlayer

data class PlayerLeaveEvent(
    val player: VulpesPlayer
)
