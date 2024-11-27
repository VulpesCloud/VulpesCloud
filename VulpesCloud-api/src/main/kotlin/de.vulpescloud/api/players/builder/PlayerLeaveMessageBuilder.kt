package de.vulpescloud.api.players.builder

import de.vulpescloud.api.players.Player
import de.vulpescloud.api.services.ClusterService

class PlayerLeaveMessageBuilder {
    companion object {
        var player: Player? = null

        fun setPlayer(player: Player): Companion {
            this.player = player
            return this
        }

        fun build(): String {
            if (player == null) {
                throw NullPointerException("Player is null!")
            }
            return "PLAYER;EVENT;LEAVE;%name%"
                .replace("%name%", player!!.name())
        }
    }
}