package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.players.v1.getAllOnlinePlayersRequest
import build.buf.gen.vulpescloud.players.v1.getOfflinePlayersRequest
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotation.Alias
import kotlinx.coroutines.launch
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
        NodeCoroutineScope.launch {
            val registeredPlayers =
                playerAPI.getAllOfflinePlayers(getOfflinePlayersRequest {}).offlinePlayersList
            source.sendMessage(
                "A total of <orange>${registeredPlayers.size}</orange> players are registered!"
            )
            if (verbose) {
                registeredPlayers.forEach { player ->
                    source.sendMessage(
                        " - <yellow>${player.name}</yellow> (<yellow>${player.uuid}</yellow>) lastSeen: <yellow>${player.lastSeen}</yellow> firstSeen: <yellow>${player.firstSeen}</yellow>"
                    )
                }
            }
        }
    }

    @Permission("players.getAllOnline")
    @Command("player|players online list")
    fun listOnlinePlayers(source: CommandSource, @Flag("v") verbose: Boolean) {
        NodeCoroutineScope.launch {
            val onlinePlayers =
                playerAPI.getAllOnlinePlayers(getAllOnlinePlayersRequest {}).onlinePlayersList

            source.sendMessage(
                "A total of <orange>${onlinePlayers.size}</orange> players are online!"
            )
            if (verbose) {
                onlinePlayers.forEach { player ->
                    source.sendMessage(
                        " - <yellow>${player.name}</yellow> (<yellow>${player.uuid}</yellow>) Proxy: <yellow>${player.proxyServiceName}</yellow> Server: <yellow>${player.serverServiceName}</yellow>"
                    )
                }
            }
        }
    }
}
