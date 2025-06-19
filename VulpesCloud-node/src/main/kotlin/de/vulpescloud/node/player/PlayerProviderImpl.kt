package de.vulpescloud.node.player

import de.vulpescloud.api.player.Player
import de.vulpescloud.api.player.PlayerProvider
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.utils.JsonUtils.getPlayer
import java.util.*
import org.json.JSONObject

class PlayerProviderImpl : PlayerProvider {
    override fun getOnlinePlayerByName(name: String): Player? {
        return getAllOnlinePlayers().find { it.name == name }
    }

    override fun getOnlinePlayerByUUID(uuid: UUID): Player? {
        return getAllOnlinePlayers().find { it.uuid == uuid }
    }

    override fun getRegisteredPlayerByName(name: String): Player? {
        return getAllRegisteredPlayers().find { it.name == name }
    }

    override fun getRegisteredPlayerByUUID(uuid: UUID): Player? {
        return getAllRegisteredPlayers().find { it.uuid == uuid }
    }

    override fun getAllRegisteredPlayers(): List<Player> {
        val players = mutableListOf<Player>()
        getRC()?.getAllHashValues("VULPESCLOUD:PLAYERS:REGISTERED")?.forEach {
            players.add(getPlayer(JSONObject(it)))
        }

        return players
    }

    override fun getAllOnlinePlayers(): List<Player> {
        val players = mutableListOf<Player>()
        getRC()?.getAllHashValues("VULPESCLOUD_PLAYERS_ONLINE")?.forEach {
            players.add(getPlayer(JSONObject(it)))
        }

        return players
    }
}
