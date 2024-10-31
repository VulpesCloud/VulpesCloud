package io.github.thecguygithub.connector.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import io.github.thecguygithub.wrapper.Wrapper
import net.kyori.adventure.text.minimessage.MiniMessage
import org.slf4j.Logger


@Plugin(id = "vulpescloud", name = "VulpesCloud-Connector", authors = ["TheCGuy"])
@Suppress("unused")
class VelocityConnector @Inject constructor(
    val logger: Logger,
    val eventManager: EventManager,
    val proxyServer: ProxyServer
){
    val wrapper = Wrapper.instance

    init {
        instance = this
    }

    @Subscribe(order = PostOrder.FIRST)
    fun start(event: ProxyInitializeEvent) {
        proxyServer.consoleCommandSource.sendMessage(MiniMessage.miniMessage().deserialize("<grey>[<aqua>VulpesCloud-Connector</aqua>]</grey> <yellow>Initializing</yellow>"))
        wrapper.getRC()?.sendMessage("SERVICE;${wrapper.service.name};EVENT;STATE;STARTING", "vulpescloud-event-service")
        VelocityRegistrationHandler
        VelocityRedisSubscribe()
    }

    @Subscribe(order = PostOrder.LAST)
    fun finishStart(event: ProxyInitializeEvent) {
        wrapper.getRC()?.sendMessage("SERVICE;${wrapper.service.name};EVENT;STATE;ONLINE", "vulpescloud-event-service")
    }

    @Subscribe(order = PostOrder.FIRST)
    fun stop(event: ProxyShutdownEvent) {
        proxyServer.consoleCommandSource.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Stopping VulpesCloud-Connector!</gray>"))
        wrapper.getRC()?.sendMessage("SERVICE;${wrapper.service.name};EVENT;STATE;STOPPING", "vulpescloud-event-service")
    }

    companion object {
        lateinit var instance: VelocityConnector
    }

}