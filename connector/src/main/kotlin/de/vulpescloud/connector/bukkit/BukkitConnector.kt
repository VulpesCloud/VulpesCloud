package de.vulpescloud.connector.bukkit

import build.buf.gen.vulpescloud.events.v1.serviceStateChangedEvent
import build.buf.gen.vulpescloud.services.v1.ServiceState
import de.vulpescloud.bridge.BridgeAPI
import de.vulpescloud.wrapper.Wrapper
import java.util.concurrent.TimeUnit
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BukkitConnector : JavaPlugin() {

    private val bridgeAPI = BridgeAPI.createFutureAPI()
    private val pluginID = 27324
    private lateinit var metrics: Metrics

    override fun onEnable() {
        metrics = Metrics(this, pluginID)

        logger.info("Publishing ServiceStateChangeEvent!")

        val localService =
            try {
                bridgeAPI.getServicesAPI().getLocalService().get(5, TimeUnit.SECONDS)
            } catch (ex: Exception) {
                logger.severe("Exception while trying to get local service!")
                logger.severe(
                    "Grpc Connection state: ${Wrapper.instance.grpcClient.channel.getState(true)}"
                )
                ex.printStackTrace()
                server.pluginManager.disablePlugin(this)
                null
            }

        if (localService == null) {
            logger.severe("LocalService is null!")
            logger.severe(
                "Grpc Connection state: ${Wrapper.instance.grpcClient.channel.getState(true)}"
            )
            server.pluginManager.disablePlugin(this)
            return
        }

        Bukkit.getPluginManager().registerEvents(PlayerJoinEventListener(), this)

        bridgeAPI
            .getEventAPI()
            .publish(
                serviceStateChangedEvent {
                    this.service = localService.toDefinition()
                    this.oldState = localService.state.toServiceState()
                    this.newState = ServiceState.SERVICE_STATE_RUNNING
                },
                true,
            )
    }

    override fun onDisable() {
        metrics.shutdown()
    }
}
