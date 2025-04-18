package de.vulpescloud.api.player

import java.util.UUID

interface PlayerProvider {

    fun getPlayerByName(name: String): Player?

    fun getPlayerByUUID(uuid: UUID): Player?

    fun getAllRegisteredPlayers(): List<Player>

    fun getAllOnlinePlayers(): List<Player>

}
