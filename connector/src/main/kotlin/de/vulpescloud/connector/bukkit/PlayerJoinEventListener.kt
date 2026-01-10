package de.vulpescloud.connector.bukkit

import build.buf.gen.vulpescloud.services.v1.UpdatePlayerCountRequest
import de.vulpescloud.bridge.BridgeAPI
import de.vulpescloud.wrapper.Wrapper
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinEventListener : Listener {

    val bridgeAPI = BridgeAPI.getCoroutineAPI()

    @EventHandler
    fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        runBlocking {
            val service = bridgeAPI.getServicesAPI().getLocalService()!!
            Wrapper.instance.grpcClient.serviceAPI.updatePlayerCount(
                UpdatePlayerCountRequest.newBuilder()
                    .setPlayerCount(Bukkit.getOnlinePlayers().size)
                    .setService(service.toDefinition())
                    .build()
            )
        }
    }
}
