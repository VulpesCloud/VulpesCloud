package de.vulpescloud.connector.velocity

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import de.vulpescloud.api.network.redis.RedisHashNames
import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.api.players.builder.PlayerJoinMessageBuilder
import de.vulpescloud.api.services.ClusterServiceFilter
import de.vulpescloud.bridge.player.PlayerImpl
import org.json.JSONObject

class VelocityEventListener {

    private val proxyServer = VelocityConnector.instance.proxyServer

    @Subscribe(order = PostOrder.FIRST)
    fun playerChooseInitialServerEvent(event: PlayerChooseInitialServerEvent) {
        val fallbackServer = VelocityConnector.instance.serviceProvider.findByFilter(ClusterServiceFilter.FALLBACKS)

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
        val player = PlayerImpl(event.player.username, event.player.uniqueId)
        player.setProxy(VelocityConnector.instance.serviceProvider.getLocalService())
        VelocityConnector.instance.wrapper.getRC()?.sendMessage(
            PlayerJoinMessageBuilder
                .setPlayer(player)
                .setService(VelocityConnector.instance.serviceProvider.getLocalService())
                .build(),
            RedisPubSubChannels.VULPESCLOUD_PLAYER_EVENT.name
        )
        val json = JSONObject(player)
        json.put("currentProxy", player.currentProxy!!.name())
        json.put("currentServer", player.currentProxy!!.name())
        VelocityConnector.instance.wrapper.getRC()?.setHashField(RedisHashNames.VULPESCLOUD_PLAYERS_ONLINE.name, event.player.uniqueId.toString(), json.toString())
    }
}