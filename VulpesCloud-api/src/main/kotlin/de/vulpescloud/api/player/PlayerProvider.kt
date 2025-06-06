package de.vulpescloud.api.player

import java.util.UUID

interface PlayerProvider {

    fun getOnlinePlayerByName(name: String): Player?

    fun getOnlinePlayerByUUID(uuid: UUID): Player?

    fun getRegisteredPlayerByName(name: String): Player?

    fun getRegisteredPlayerByUUID(uuid: UUID): Player?

    fun getAllRegisteredPlayers(): List<Player>

    fun getAllOnlinePlayers(): List<Player>

}
