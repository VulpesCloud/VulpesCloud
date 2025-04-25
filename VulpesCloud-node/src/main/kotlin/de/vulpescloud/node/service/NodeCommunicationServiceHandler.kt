package de.vulpescloud.node.service

import de.vulpescloud.api.service.ServiceActions
import de.vulpescloud.api.service.ServiceProvider
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

object NodeCommunicationServiceHandler : KoinComponent {

    private val serviceProvider: ServiceProvider by inject()
    private val serviceProviderImpl = serviceProvider as ServiceProviderImpl

    private val logger = LoggerFactory.getLogger(javaClass)

    fun handleServiceMessage(msg: JSONObject) {
        when (ServiceActions.valueOf(msg.getString("action"))) {
            ServiceActions.START -> {
                val localService = serviceProviderImpl.localServices.find { it.name == msg.getString("service") }
                if (localService == null) {
                    logger.error("Received message to start service ${msg.getString("service")}, but it wasn't found in the LocalServices!")
                    return
                } else {
                    logger.debug("Received message to start service ${localService.name}")
                    localService.start()
                }
            }
            ServiceActions.STOP ->  {
                val localService = serviceProviderImpl.localServices.find { it.name == msg.getString("service") }
                if (localService == null) {
                    logger.error("Received message to stop service ${msg.getString("service")}, but it wasn't found in the LocalServices!")
                    return
                } else {
                    logger.debug("Received message to stop service ${localService.name}")
                    localService.sendCommand("stop")
                }
            }
            ServiceActions.KILL ->  {
                val localService = serviceProviderImpl.localServices.find { it.name == msg.getString("service") }
                if (localService == null) {
                    logger.error("Received message to kill service ${msg.getString("service")}, but it wasn't found in the LocalServices!")
                    return
                } else {
                    logger.debug("Received message to kill service ${localService.name}")
                    localService.forceStop()
                }
            }
            ServiceActions.RESTART -> {
                logger.warn("Service Restarting is not yet implemented!")
            }
        }
    }

}
