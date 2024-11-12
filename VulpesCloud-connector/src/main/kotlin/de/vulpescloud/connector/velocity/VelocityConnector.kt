package de.vulpescloud.connector.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.connector.Connector
import net.kyori.adventure.text.minimessage.MiniMessage
import org.slf4j.Logger


@Plugin(id = "vulpescloud", name = "VulpesCloud-Connector", authors = ["TheCGuy"])
@Suppress("unused")
class VelocityConnector @Inject constructor(
    val logger: Logger,
    val eventManager: EventManager,
    val proxyServer: ProxyServer,
) : Connector() {

    init {
        instance = this
    }

    @Subscribe(order = PostOrder.FIRST)
    fun start(event: ProxyInitializeEvent) {
        proxyServer.consoleCommandSource.sendMessage(
            MiniMessage.miniMessage()
                .deserialize("<grey>[<aqua>VulpesCloud-Connector</aqua>]</grey> <yellow>Initializing</yellow>")
        )
        init()
        VelocityRegistrationHandler
        VelocityRedisSubscribe()
    }

    @Subscribe(order = PostOrder.LAST)
    fun finishStart(event: ProxyInitializeEvent) {
        finishStart()
    }

    @Subscribe(order = PostOrder.FIRST)
    fun stop(event: ProxyShutdownEvent) {
        proxyServer.consoleCommandSource.sendMessage(
            MiniMessage.miniMessage().deserialize("<gray>Stopping VulpesCloud-Connector!</gray>")
        )
        shutdownLocal()
    }

    companion object {
        lateinit var instance: VelocityConnector
    }

}