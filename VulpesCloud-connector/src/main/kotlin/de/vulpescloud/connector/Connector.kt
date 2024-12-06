package de.vulpescloud.connector

import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.bridge.service.ServiceProvider
import de.vulpescloud.wrapper.Wrapper

open class Connector {

    val wrapper = Wrapper.instance

    fun init() {
        // serviceProvider.getAllServiceFromRedis()
        // playerProvider.initializePlayerProvider()
        // playerProvider.loadPlayerDataFromRedis()

        // TimeUnit.SECONDS.sleep(1)

        ServiceProvider.findServiceById(Wrapper.instance.service.id)?.setState(ServiceStates.STARTING)
    }

    fun finishStart() {
        ServiceProvider.findServiceById(Wrapper.instance.service.id)?.setState(ServiceStates.ONLINE)
    }

    fun shutdownLocal() {
        ServiceProvider.findServiceById(Wrapper.instance.service.id)?.setState(ServiceStates.STOPPING)
    }

}