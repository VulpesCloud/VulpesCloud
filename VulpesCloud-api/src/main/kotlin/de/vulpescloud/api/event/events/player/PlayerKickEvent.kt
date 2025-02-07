package de.vulpescloud.api.event.events.player

import de.vulpescloud.api.player.VulpesPlayer

data class PlayerKickEvent(
    val player: VulpesPlayer,
    val reason: String
)
