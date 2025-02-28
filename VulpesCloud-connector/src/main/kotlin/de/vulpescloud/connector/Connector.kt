package de.vulpescloud.connector

import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.bridge.service.ServiceProvider
import de.vulpescloud.wrapper.Wrapper

open class Connector {

    val wrapper = Wrapper.instance

    fun init() {
        ServiceProvider.findServiceById(Wrapper.instance.serviceUUID)?.updateState(ServiceStates.STARTING)
    }

    fun finishStart() {
        ServiceProvider.findServiceById(Wrapper.instance.serviceUUID)?.updateState(ServiceStates.ONLINE)
    }

    fun shutdownLocal() {
        ServiceProvider.findServiceById(Wrapper.instance.serviceUUID)?.updateState(ServiceStates.STOPPING)
    }

}