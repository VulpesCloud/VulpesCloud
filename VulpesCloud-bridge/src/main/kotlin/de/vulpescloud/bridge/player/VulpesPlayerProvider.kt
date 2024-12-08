package de.vulpescloud.bridge.player

import de.vulpescloud.api.player.VulpesPlayer
import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.bridge.json.PlayerSerializer.playerFromJson
import de.vulpescloud.wrapper.Wrapper
import org.json.JSONObject
import org.slf4j.LoggerFactory

class VulpesPlayerProvider {
    private val logger = LoggerFactory.getLogger(VulpesPlayerProvider::class.java)

    fun players(): List<VulpesPlayer> {
        val playerList = mutableListOf<VulpesPlayer>()
        val list = Wrapper.instance.getRC()?.getAllHashValues(RedisHashNames.VULPESCLOUD_PLAYERS.name)
        if (!list.isNullOrEmpty()) {
            list.forEach {
                val player = playerFromJson(JSONObject(it))
                playerList.add(player)
            }
        }
        return playerList
    }

    fun onlinePlayers(): List<VulpesPlayer> {
        val playerList = mutableListOf<VulpesPlayer>()
        val list = Wrapper.instance.getRC()?.getAllHashValues(RedisHashNames.VULPESCLOUD_PLAYERS_ONLINE.name)
        if (!list.isNullOrEmpty()) {
            list.forEach {
                val player = playerFromJson(JSONObject(it))
                playerList.add(player)
            }
        }
        return playerList
    }

    //todo Add Stuff like checking if a player is online and getting Players from Name and uuid and maybe Filter them by what service they are on
}