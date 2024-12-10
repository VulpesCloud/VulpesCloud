package de.vulpescloud.connector.velocity

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import de.vulpescloud.api.player.builder.PlayerJoinMessageBuilder
import de.vulpescloud.api.player.builder.PlayerLeaveMessageBuilder
import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.api.services.ServiceFilters
import de.vulpescloud.bridge.player.VulpesPlayerImpl
import de.vulpescloud.bridge.player.VulpesPlayerProvider
import de.vulpescloud.bridge.service.ServiceProvider
import de.vulpescloud.wrapper.Wrapper
import org.json.JSONObject

class VelocityEventListener {

    private val proxyServer = VelocityConnector.instance.proxyServer

    @Subscribe(order = PostOrder.FIRST)
    fun playerChooseInitialServerEvent(event: PlayerChooseInitialServerEvent) {
        val fallbackServer = ServiceProvider.findServicesByFilter(ServiceFilters.FALLBACKS)

        if (fallbackServer?.isEmpty()!!) {
            println("FALLBACK SERVER IS NULL!")
            event.setInitialServer(null)
            return
        }

        //todo maybe send redis Message if there is no fallback
        println(fallbackServer[0].name())
        proxyServer.getServer(fallbackServer[0].name()).ifPresent { event.setInitialServer(it) }
    }

    @Subscribe
    fun playerPostLoginEvent(event: PostLoginEvent) {
        val player = VulpesPlayerImpl(event.player.username, event.player.uniqueId) // ServiceProvider.findServiceById(Wrapper.instance.service.id), ServiceProvider.findServiceByName(event.player.currentServer.get().serverInfo.name),
        VelocityConnector.instance.wrapper.getRC()?.sendMessage(
            PlayerJoinMessageBuilder
                .setPlayer(player)
                .setService(ServiceProvider.findServiceById(Wrapper.instance.service.id)!!)
                .build(),
            RedisChannelNames.VULPESCLOUD_PLAYER_EVENT.name
        )
        val json = JSONObject(player)
        VelocityConnector.instance.wrapper.getRC()?.setHashField(RedisHashNames.VULPESCLOUD_PLAYERS.name, event.player.uniqueId.toString(), json.toString())
        VelocityConnector.instance.wrapper.getRC()?.setHashField(RedisHashNames.VULPESCLOUD_PLAYERS_ONLINE.name, event.player.uniqueId.toString(), json.toString())
    }

    @Subscribe
    fun playerQuitEvent(event: DisconnectEvent) {
        VelocityConnector.instance.wrapper.getRC()?.sendMessage(
            PlayerLeaveMessageBuilder
                .setPlayer(VulpesPlayerProvider.getOnlinePlayerByName(event.player.username)!!)
                .build(),
            RedisChannelNames.VULPESCLOUD_PLAYER_EVENT.name
        )
        VelocityConnector.instance.wrapper.getRC()?.deleteHashField(RedisHashNames.VULPESCLOUD_PLAYERS_ONLINE.name, event.player.uniqueId.toString())
    }

}