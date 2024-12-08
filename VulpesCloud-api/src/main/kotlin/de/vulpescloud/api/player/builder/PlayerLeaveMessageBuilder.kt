package de.vulpescloud.api.player.builder

import de.vulpescloud.api.player.VulpesPlayer

class PlayerLeaveMessageBuilder {
    companion object {
        var player: VulpesPlayer? = null

        fun setPlayer(player: VulpesPlayer): Companion {
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