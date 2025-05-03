package de.vulpescloud.node.commands

import de.vulpescloud.api.player.PlayerProvider
import org.incendo.cloud.annotations.Command

@Suppress("unused")
class PlayerCommand(
    private val playerProvider: PlayerProvider
) {

    @Command("player|players online list")
    fun listOnlinePlayers() {
        val onlinePlayers = playerProvider.getAllOnlinePlayers()
        println("A total of ${onlinePlayers.size} player(s) are online:")
        onlinePlayers.forEach { player ->
            println(" &8- &m${player.name} &7Proxy: &8${player.currentProxy} &7Service: &8${player.currentServer} &7UUID: &8${player.uuid}")
        }
    }

    @Command("player|players registered list")
    fun listRegisteredPlayers() {
        val registeredPlayers = playerProvider.getAllRegisteredPlayers()
        println("A total of ${registeredPlayers.size} player(s) are registered:")
        registeredPlayers.forEach { player ->
            println(" &8- &m${player.name} &7UUID: &8${player.uuid}")
        }
    }

}
