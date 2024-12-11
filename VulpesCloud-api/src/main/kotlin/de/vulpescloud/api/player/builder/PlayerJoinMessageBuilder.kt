package de.vulpescloud.api.player.builder

import de.vulpescloud.api.player.VulpesPlayer
import de.vulpescloud.api.services.Service

class PlayerJoinMessageBuilder {
    companion object {
        var player: VulpesPlayer? = null
        var service: Service? = null

        fun setPlayer(player: VulpesPlayer): Companion {
            this.player = player
            return this
        }

        fun setService(service: Service): Companion {
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

            return "PLAYER;EVENT;JOIN;%name%;%uuid%;PROXY;%service%"
                .replace("%name%", player!!.name())
                .replace("%service%", service!!.name())
                .replace("%uuid%", player!!.uuid().toString())
        }
    }
}