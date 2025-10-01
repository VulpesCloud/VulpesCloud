package de.vulpescloud.connector.velocity

import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.api.events.services.ServiceStateChangeEvent
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.bridge.BridgeAPI
import de.vulpescloud.connector.velocity.events.PlayerChooseInitialServerEventListener
import de.vulpescloud.wrapper.Wrapper
import jakarta.inject.Inject
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

        velocityServerRegistrationHandler =
            VelocityServerRegistrationHandler(proxyServer, bridgeAPI)

        eventManager.register(this, PlayerChooseInitialServerEventListener(bridgeAPI, proxyServer))

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
}
