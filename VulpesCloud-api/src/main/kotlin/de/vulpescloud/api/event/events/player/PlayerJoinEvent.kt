package de.vulpescloud.api.event.events.player

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.player.VulpesPlayer

data class PlayerJoinEvent(
    val player: VulpesPlayer
) : Event {
    override fun name(): String {
        return "PlayerJoinEvent"
    }
}
