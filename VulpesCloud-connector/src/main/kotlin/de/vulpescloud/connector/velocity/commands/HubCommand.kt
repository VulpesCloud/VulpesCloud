package de.vulpescloud.connector.velocity.commands

import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.api.services.ServiceFilters
import de.vulpescloud.bridge.service.ServiceProvider
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import dev.jorel.commandapi.kotlindsl.commandTree

class HubCommand(proxy: ProxyServer) {

    val command = commandTree("hub") {
        withAliases("lobby", "l", "leave")
        executesPlayer(
            PlayerCommandExecutor { sender, _ ->
                val fallbackServer = ServiceProvider.findServicesByFilter(ServiceFilters.FALLBACKS)

                sender.createConnectionRequest(proxy.getServer(fallbackServer!![0].name()).get()).connectWithIndication()
            }
        )
    }

}