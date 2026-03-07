package de.vulpescloud.connector.velocity

import build.buf.gen.vulpescloud.services.v1.UpdatePlayerCountRequest
import build.buf.gen.vulpescloud.virtualconfig.v1.createVirtualConfigRequest
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.api.events.services.ServiceStateChangeEvent
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.bridge.BridgeAPI
import de.vulpescloud.connector.velocity.commands.CloudCommand
import de.vulpescloud.connector.velocity.commands.HubCommand
import de.vulpescloud.connector.velocity.config.ConnectorConfig
import de.vulpescloud.connector.velocity.events.PlayerChooseInitialServerEventListener
import de.vulpescloud.wrapper.Wrapper
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIVelocityConfig
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.bstats.velocity.Metrics
import org.slf4j.Logger
import java.util.concurrent.TimeUnit

@Plugin(id = "vulpescloud-connector", name = "VulpesCloud-Connector", authors = ["TheCGuy"])
@Suppress("unused")
class VelocityConnector
@Inject
constructor(
    private val eventManager: EventManager,
    private val proxyServer: ProxyServer,
    private val metricsFactory: Metrics.Factory,
    private val logger: Logger,
) {
    private lateinit var metrics: Metrics
    private val pluginID = 27325
    private val bridgeAPI = BridgeAPI.getFutureAPI()
    private lateinit var velocityServerRegistrationHandler: VelocityServerRegistrationHandler

    @Subscribe
    fun onProxyInitializeEvent(event: ProxyInitializeEvent) {
        metrics = metricsFactory.make(this, pluginID)

        CommandAPI.onLoad(CommandAPIVelocityConfig(proxyServer, this))

        velocityServerRegistrationHandler =
            VelocityServerRegistrationHandler(proxyServer, bridgeAPI)

        eventManager.register(this, PlayerChooseInitialServerEventListener(bridgeAPI, proxyServer))

        runBlocking {
            BridgeAPI.getCoroutineAPI()
                .getVirtualConfigAPI()
                .stub
                .createVirtualConfig(
                    createVirtualConfigRequest {
                        this.name = "vc_connector"
                        this.config = Json.encodeToString(ConnectorConfig())
                    }
                )
        }

        val localService =
            try {
                bridgeAPI.getServicesAPI().getLocalService().get(5, TimeUnit.SECONDS)
            } catch (ex: Exception) {
                logger.error("Exception while trying to get local service!")
                logger.error(
                    "Grpc Connection state: ${Wrapper.instance.grpcClient.channel.getState(true)}"
                )
                ex.printStackTrace()
                null
            }

        if (localService == null) {
            logger.error("LocalService is null!")
            logger.error(
                "Grpc Connection state: ${Wrapper.instance.grpcClient.channel.getState(true)}"
            )
            return
        }

        HubCommand(proxyServer).command.register()
        CloudCommand()

        bridgeAPI
            .getEventAPI()
            .publish(
                ServiceStateChangeEvent(localService, localService.state, ServiceStates.RUNNING),
                true,
            )
    }

    @Subscribe
    fun onProxyShutdownEvent(event: ProxyShutdownEvent) {
        metrics.shutdown()
        velocityServerRegistrationHandler.shutdown()
    }

    @Subscribe
    fun onLoginEvent(event: LoginEvent) {
        runBlocking {
            val service = bridgeAPI.getServicesAPI().getLocalService().get()!!
            Wrapper.instance.grpcClient.serviceAPI.updatePlayerCount(
                UpdatePlayerCountRequest.newBuilder()
                    .setPlayerCount(proxyServer.playerCount)
                    .setService(service.toDefinition())
                    .build()
            )
        }
    }

    @Subscribe
    fun onDisconnectEvent(event: DisconnectEvent) {
        runBlocking {
            val service = bridgeAPI.getServicesAPI().getLocalService().get()!!
            Wrapper.instance.grpcClient.serviceAPI.updatePlayerCount(
                UpdatePlayerCountRequest.newBuilder()
                    .setPlayerCount(proxyServer.playerCount)
                    .setService(service.toDefinition())
                    .build()
            )
        }
    }
}
