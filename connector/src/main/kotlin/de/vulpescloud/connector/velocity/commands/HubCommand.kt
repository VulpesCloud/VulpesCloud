package de.vulpescloud.connector.velocity.commands

import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.bridge.BridgeAPI
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import java.util.concurrent.TimeUnit

class HubCommand(proxyServer: ProxyServer) {

    val command =
        CommandTree("hub")
            .withAliases("lobby", "l", "leave")
            .executesPlayer(
                PlayerCommandExecutor { sender, _ ->
                    val fallbackServer =
                        BridgeAPI.getFutureAPI()
                            .getServicesAPI()
                            .getAllServices()
                            .get(5, TimeUnit.SECONDS)
                            .filter { it.task.fallback }

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
