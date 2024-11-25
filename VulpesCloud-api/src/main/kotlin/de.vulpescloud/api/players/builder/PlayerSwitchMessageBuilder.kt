package de.vulpescloud.api.players.builder

import de.vulpescloud.api.players.Player
import de.vulpescloud.api.services.ClusterService

class PlayerSwitchMessageBuilder {
    companion object {
        var player: Player? = null
        var service: ClusterService? = null

        fun setPlayer(player: Player): Companion {
            this.player = player
            return this
        }

        fun setService(service: ClusterService): Companion {
            this.service = service
            return this
        }

        fun build(): String {
            if (player == null) {
                throw NullPointerException("Player is null!")
            }
            if (service == null) {
                throw NullPointerException("Service is null!")
            }

            return "PLAYER;EVENT;SWITCH;%name%;PROXY;%service%"
                .replace("%name%", player!!.name())
                .replace("%service%", service!!.name())
        }
    }
}