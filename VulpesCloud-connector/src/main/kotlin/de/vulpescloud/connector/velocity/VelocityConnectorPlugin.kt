package de.vulpescloud.connector.velocity

import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.api.event.events.player.PlayerJoinEvent
import de.vulpescloud.api.event.events.player.PlayerLeaveEvent
import de.vulpescloud.api.player.Player
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.api.service.ServiceFilter
import de.vulpescloud.bridge.VulpesBridge
import de.vulpescloud.bridge.VulpesBridge.getEventManager
import de.vulpescloud.bridge.VulpesBridge.getServiceProvider
import de.vulpescloud.connector.common.Connector
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import jakarta.inject.Inject
import org.json.JSONObject

@Plugin(id = "vulpescloud-connector", name = "VulpesCloud-Connector", authors = ["TheCGuy"])
@Suppress("unused")
class VelocityConnectorPlugin
@Inject
constructor(
    private val eventManager: EventManager,
    private val proxyServer: ProxyServer,
    private val pluginsContainer: PluginContainer,
) : Connector {

    private lateinit var velocityServerRegistrationHandler: VelocityServerRegistrationHandler

    @Subscribe(order = PostOrder.LAST)
    fun onLastProxyInitialize(event: ProxyInitializeEvent) {

        velocityServerRegistrationHandler = VelocityServerRegistrationHandler(proxyServer)
        getEventManager().registerListener(velocityServerRegistrationHandler)

        markOnline()
    }

    @Subscribe(order = PostOrder.FIRST)
    fun onProxyShutdownEvent(event: ProxyShutdownEvent) {
        markStopping()
    }

    @Subscribe(order = PostOrder.FIRST)
    fun playerChooseInitialServerEvent(event: PlayerChooseInitialServerEvent) {
        val fallbackServer = getServiceProvider().getServicesByFilter(ServiceFilter.FALLBACKS)

        if (fallbackServer.isEmpty()) {
            println("FALLBACK SERVER IS NULL!")
            event.setInitialServer(null)
            return
        }

        // todo get lowest fallback server
        proxyServer.getServer(fallbackServer[0].name).ifPresent { event.setInitialServer(it) }

        val player =
            Player(
                event.player.username,
                event.player.uniqueId,
                getServiceProvider().getLocalService().name,
                fallbackServer[0].name,
            )

        getRC()
            ?.setHashField("VULPESCLOUD_PLAYERS_ONLINE", player.name, JSONObject(player).toString())
        getRC()
            ?.setHashField(
                "VULPESCLOUD_PLAYERS_REGISTERED",
                player.name,
                JSONObject(player).toString(),
            )

        getEventManager()
            .callGlobal(
                PlayerJoinEvent(player),
                RedisChannels.VULPESCLOUD_EVENT_PLAYER_PlayerJoinEvent,
            )
    }

    @Subscribe
    fun onDisconnectEvent(event: DisconnectEvent) {
        val player = Player(event.player.username, event.player.uniqueId, "N/A", "N/A")

        getRC()?.deleteHashField("VULPESCLOUD_PLAYERS_ONLINE", player.name)

        getEventManager()
            .callGlobal(
                PlayerLeaveEvent(player),
                RedisChannels.VULPESCLOUD_EVENT_PLAYER_PlayerLeaveEvent,
            )
    }
}
