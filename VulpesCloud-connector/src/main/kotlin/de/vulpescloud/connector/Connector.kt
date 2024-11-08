package de.vulpescloud.connector

import de.vulpescloud.bridge.service.ServiceProvider

open class Connector {

    val serviceProvider = ServiceProvider()

    init {
        serviceProvider.getAllServiceFromRedis()
    }

}