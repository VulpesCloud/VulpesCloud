package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.players.v1.getAllOnlinePlayersRequest
import build.buf.gen.vulpescloud.players.v1.getOfflinePlayersRequest
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotation.Alias
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission

@Command("players")
@Alias(["player"])
class PlayersCommand {
    private val playerAPI by lazy { Node.instance.localGrpcClient.playerAPI }

    @Permission("players.getAllOffline")
    @Command("player|players registered list")
    fun listRegisteredPlayers(source: CommandSource, @Flag("v") verbose: Boolean) {
        runBlocking {
            val registeredPlayers =
                playerAPI.getAllOfflinePlayers(getOfflinePlayersRequest {}).offlinePlayersList
            source.sendMessage(
                "<gray>A total of</gray> <gold>${registeredPlayers.size}</gold> <gray>players are registered!</gray>"
            )
            if (verbose) {
                registeredPlayers.forEach { player ->
                    source.sendMessage(
                        " <dark_gray>»</dark_gray> <white>${player.name}</white> <dark_gray>(</dark_gray><gray>${player.uuid}</gray><dark_gray>)</dark_gray> <gray>lastSeen:</gray> <white>${player.lastSeen}</white> <gray>firstSeen:</gray> <white>${player.firstSeen}</white>"
                    )
                }
            }
        }
    }

    @Permission("players.getAllOnline")
    @Command("player|players online list")
    fun listOnlinePlayers(source: CommandSource, @Flag("v") verbose: Boolean) {
        runBlocking {
            val onlinePlayers =
                playerAPI.getAllOnlinePlayers(getAllOnlinePlayersRequest {}).onlinePlayersList

            source.sendMessage(
                "<gray>A total of</gray> <gold>${onlinePlayers.size}</gold> <gray>players are online!</gray>"
            )
            if (verbose) {
                onlinePlayers.forEach { player ->
                    source.sendMessage(
                        " <dark_gray>»</dark_gray> <white>${player.name}</white> <dark_gray>(</dark_gray><gray>${player.uuid}</gray><dark_gray>)</dark_gray> <gray>Proxy:</gray> <white>${player.proxyServiceName}</white> <gray>Server:</gray> <white>${player.serverServiceName}</white>"
                    )
                }
            }
        }
    }
}
