package de.vulpescloud.connector.velocity

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.connector.Connector
import de.vulpescloud.connector.velocity.commands.HubCommand
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIVelocityConfig
import jakarta.inject.Inject
import net.kyori.adventure.text.minimessage.MiniMessage
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.velocity.VelocityCommandManager

@Plugin(id = "vulpescloud", name = "VulpesCloud-Connector", authors = ["TheCGuy"])
@Suppress("unused")
class VelocityConnector @Inject constructor(
    val eventManager: EventManager,
    val proxyServer: ProxyServer,
    val pluginsContainer: PluginContainer,
) : Connector() {

    private lateinit var commandManager: VelocityCommandManager<CommandSource>

    init {
        instance = this
    }

    @Subscribe(order = PostOrder.FIRST)
    fun proxyInitializeEventFIRST(event: ProxyInitializeEvent) {
        proxyServer.consoleCommandSource.sendMessage(
            MiniMessage.miniMessage()
                .deserialize("<grey>[<aqua>VulpesCloud-Connector</aqua>]</grey> <yellow>Initializing</yellow>")
        )
        init()

        CommandAPI.onLoad(CommandAPIVelocityConfig(proxyServer, this))

        commandManager = VelocityCommandManager(
            pluginsContainer,
            proxyServer,
            ExecutionCoordinator.asyncCoordinator(),
            SenderMapper.identity()
        )

        VelocityRegistrationHandler
        VelocityRedisListener()
        this.eventManager.register(this, VelocityEventListener())
    }

    @Subscribe(order = PostOrder.LAST)
    fun proxyInitializeEventLAST(event: ProxyInitializeEvent) {

        CommandAPI.onEnable()

        // CloudCommand()
        HubCommand(proxyServer)

        finishStart()
    }

    @Subscribe(order = PostOrder.FIRST)
    fun stop(event: ProxyShutdownEvent) {
        CommandAPI.onDisable()
        proxyServer.consoleCommandSource.sendMessage(
            MiniMessage.miniMessage().deserialize("<gray>Stopping VulpesCloud-Connector!</gray>")
        )
        shutdownLocal()
    }

    companion object {
        lateinit var instance: VelocityConnector
    }
}