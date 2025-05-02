package de.vulpescloud.connector.velocity

import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.bridge.VulpesBridge
import de.vulpescloud.connector.common.Connector
import jakarta.inject.Inject

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
        VulpesBridge.getEventManager().registerListener(velocityServerRegistrationHandler)

        markOnline()
    }

    @Subscribe(order = PostOrder.FIRST)
    fun onProxyShutdownEvent(event: ProxyShutdownEvent) {
        markStopping()
    }
}
