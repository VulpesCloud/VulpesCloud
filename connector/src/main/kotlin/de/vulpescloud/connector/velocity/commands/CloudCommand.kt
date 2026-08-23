package de.vulpescloud.connector.velocity.commands

import build.buf.gen.vulpescloud.auth.v1.getUserByExtraDataRequest
import build.buf.gen.vulpescloud.cluster.v2.commandTabCompleteRequest
import build.buf.gen.vulpescloud.cluster.v2.executeCommandRequest
import build.buf.gen.vulpescloud.cluster.v2.playerCommandSource
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import de.vulpescloud.bridge.BridgeAPI
import de.vulpescloud.connector.velocity.config.getConfig
import de.vulpescloud.wrapper.Wrapper
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

class CloudCommand(val bridgeAPI: BridgeAPI.BridgeCoroutineAPI) : SimpleCommand {
    private val coroutinesScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val miniMessage = MiniMessage.miniMessage()

    override fun execute(invocation: SimpleCommand.Invocation) {
        if (invocation.source() !is Player) {
            invocation
                .source()
                .sendMessage(Component.text("This command can only be executed by a player!"))
            return
        }

        runBlocking {
            val config = getConfig()
            Wrapper.instance.grpcClient.clusterAPI
                .executeCommand(
                    executeCommandRequest {
                        this.command = invocation.arguments().joinToString(" ")
                        this.playerSource = playerCommandSource {
                            this.playerName = (invocation.source() as Player).username
                            this.playerUuid = (invocation.source() as Player).uniqueId.toString()
                            this.playerProxyServiceName =
                                bridgeAPI.getServicesAPI().getLocalService()!!.name()
                            this.playerServerServiceName =
                                (invocation.source() as Player).currentServer.get().serverInfo.name
                        }
                    }
                )
                .outputList
                .forEach {
                    invocation.source().sendMessage(miniMessage.deserialize("${config.prefix} $it"))
                }
        }
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
        return runBlocking {
            val permissions =
                Wrapper.instance.grpcClient.authAPI
                    .getUserByExtraData(
                        getUserByExtraDataRequest {
                            this.key = "minecraft-uuid"
                            this.value = (invocation.source() as Player).uniqueId.toString()
                        }
                    )
                    .user
                    .permissionsList

            permissions.contains("*") || permissions.contains("cloud.command")
        }
    }

    override fun suggestAsync(
        invocation: SimpleCommand.Invocation
    ): CompletableFuture<List<String>> {
        return coroutinesScope.future {
            val player = invocation.source() as Player

            Wrapper.instance.grpcClient.clusterAPI
                .commandTabComplete(
                    commandTabCompleteRequest {
                        this.command = invocation.arguments().joinToString(" ")
                        this.playerSource = playerCommandSource {
                            this.playerName = player.username
                            this.playerUuid = player.uniqueId.toString()
                            this.playerProxyServiceName =
                                bridgeAPI.getServicesAPI().getLocalService()!!.name()
                            this.playerServerServiceName =
                                player.currentServer.get().serverInfo.name
                        }
                    }
                )
                .suggestionsList
        }
    }
}
