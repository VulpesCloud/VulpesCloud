package de.vulpescloud.node.commands

import de.vulpescloud.api.player.PlayerProvider
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotations.Description
import org.incendo.cloud.annotations.Command

@Suppress("unused")
@Description("COMMANDS.DESCRIPTION.player")
class PlayerCommand(private val playerProvider: PlayerProvider) {

    @Command("player|players online list")
    fun listOnlinePlayers(source: CommandSource) {
        val onlinePlayers = playerProvider.getAllOnlinePlayers()
        source.sendMessage("A total of ${onlinePlayers.size} player(s) are online:")
        onlinePlayers.forEach { player ->
            source.sendMessage(
                " &8- &m${player.name} &7Proxy: &8${player.currentProxy} &7Service: &8${player.currentServer} &7UUID: &8${player.uuid}"
            )
        }
    }

    @Command("player|players registered list")
    fun listRegisteredPlayers(source: CommandSource) {
        val registeredPlayers = playerProvider.getAllRegisteredPlayers()
        source.sendMessage("A total of ${registeredPlayers.size} player(s) are registered:")
        registeredPlayers.forEach { player ->
            source.sendMessage(" &8- &m${player.name} &7UUID: &8${player.uuid}")
        }
    }
}
