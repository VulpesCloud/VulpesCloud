package de.vulpescloud.connector.velocity.commands

import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.bridge.BridgeAPI
import de.vulpescloud.connector.velocity.config.getConfig
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.minimessage.MiniMessage
import org.slf4j.LoggerFactory

class HubCommand(proxyServer: ProxyServer, bridgeAPI: BridgeAPI.BridgeFutureAPI) {

    private val logger = LoggerFactory.getLogger("HubCommand")
    private val miniMessage = MiniMessage.miniMessage()

    val command =
        CommandTree("hub")
            .withAliases("lobby", "l", "leave")
            .executesPlayer(
                PlayerCommandExecutor { sender, _ ->
                    val fallbackServer =
                        bridgeAPI
                            .getServicesAPI()
                            .getAllServices()
                            .get(5, TimeUnit.SECONDS)
                            .filter { it.task.fallback }

                    if (fallbackServer.isEmpty()) {
                        logger.error("No fallback server found!")
                        runBlocking {
                            sender.sendMessage(
                                miniMessage.deserialize(
                                    getConfig().disconnectNoAvailableServerMessage
                                )
                            )
                        }
                        return@PlayerCommandExecutor
                    }

                    sender
                        .createConnectionRequest(
                            proxyServer
                                .getServer(
                                    "${fallbackServer[0].task.name}-${fallbackServer[0].orderedId}"
                                )
                                .get()
                        )
                        .connectWithIndication()
                }
            )
}
