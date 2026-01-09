package de.vulpescloud.connector.bukkit

import de.vulpescloud.api.events.services.ServiceStateChangeEvent
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.bridge.BridgeAPI
import de.vulpescloud.wrapper.Wrapper
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.TimeUnit

class BukkitConnector : JavaPlugin() {

    private val bridgeAPI = BridgeAPI.getFutureAPI()
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

        bridgeAPI
            .getEventAPI()
            .publish(
                ServiceStateChangeEvent(localService, localService.state, ServiceStates.RUNNING)
            )
    }

    override fun onDisable() {
        metrics.shutdown()
    }
}
