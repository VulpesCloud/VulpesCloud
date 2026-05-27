package de.vulpescloud.connector.velocity.events

import build.buf.gen.vulpescloud.events.v1.PlayerActionEvent
import build.buf.gen.vulpescloud.events.v1.PlayerActions
import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.bridge.BridgeAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import org.slf4j.LoggerFactory

class VelocityPlayerActionEventListener(
    proxyServer: ProxyServer,
    bridgeAPI: BridgeAPI.BridgeCoroutineAPI,
) {
    private val minimessage = MiniMessage.miniMessage()
    private val logger = LoggerFactory.getLogger(VelocityPlayerActionEventListener::class.java)

    init {
        bridgeAPI.getEventAPI().subscribe<PlayerActionEvent> { event ->
            if (proxyServer.getPlayer(event.player.name).isPresent) {
                when (event.action) {
                    PlayerActions.KICK -> {
                        val reason = event.dataMap["reason"]
                        proxyServer
                            .getPlayer(event.player.name)
                            .get()
                            .disconnect(minimessage.deserialize(reason.toString()))
                    }
                    PlayerActions.MESSAGE -> {
                        val message = event.dataMap["message"]
                        proxyServer
                            .getPlayer(event.player.name)
                            .get()
                            .sendMessage(minimessage.deserialize(message.toString()))
                    }
                    PlayerActions.TITLE -> {
                        val title = minimessage.deserialize(event.dataMap["title"].toString())
                        val subtitle = minimessage.deserialize(event.dataMap["subtitle"].toString())
                        val finalTitle = Title.title(title, subtitle)
                        proxyServer.getPlayer(event.player.name).get().showTitle(finalTitle)
                    }
                    PlayerActions.ACTION_BAR -> {
                        val message = event.dataMap["message"]
                        proxyServer
                            .getPlayer(event.player.name)
                            .get()
                            .sendActionBar(minimessage.deserialize(message.toString()))
                    }
                    PlayerActions.CONNECT -> {
                        val targetServer = event.dataMap["targetServer"]
                        proxyServer
                            .getPlayer(event.player.name)
                            .get()
                            .createConnectionRequest(proxyServer.getServer(targetServer).get())
                    }
                    PlayerActions.UNRECOGNIZED -> logger.error("Player Action is unrecognized!")
                    PlayerActions.UNSPECIFIED -> logger.error("Player Action is unspecified!")
                }
            }
        }
    }
}
