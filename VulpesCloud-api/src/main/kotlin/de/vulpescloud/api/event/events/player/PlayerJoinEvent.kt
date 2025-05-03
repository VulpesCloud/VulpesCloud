package de.vulpescloud.api.event.events.player

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.player.Player

data class PlayerJoinEvent(val player: Player) : Event
