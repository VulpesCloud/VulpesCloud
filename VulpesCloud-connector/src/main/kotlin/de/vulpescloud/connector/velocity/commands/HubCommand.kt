package de.vulpescloud.connector.velocity.commands

import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.api.service.ServiceFilter
import de.vulpescloud.bridge.VulpesBridge.getServiceProvider
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import dev.jorel.commandapi.kotlindsl.commandTree

class HubCommand(proxy: ProxyServer) {

    val command =
        commandTree("hub") {
            withAliases("lobby", "l", "leave")
            executesPlayer(
                PlayerCommandExecutor { sender, _ ->
                    val fallbackServer =
                        getServiceProvider().getServicesByFilter(ServiceFilter.FALLBACKS)

                    sender
                        .createConnectionRequest(proxy.getServer(fallbackServer[0].name).get())
                        .connectWithIndication()
                }
            )
        }
}
