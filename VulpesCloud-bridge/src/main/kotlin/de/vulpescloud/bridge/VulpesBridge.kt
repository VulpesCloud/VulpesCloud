package de.vulpescloud.bridge

import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.bridge.event.EventManagerImpl
import de.vulpescloud.bridge.service.ServiceProviderImpl

object VulpesBridge {

    private val serviceProvider: ServiceProvider = ServiceProviderImpl()
    private val eventManager: EventManager = EventManagerImpl()

    fun getServiceProvider(): ServiceProviderImpl = serviceProvider as ServiceProviderImpl

    fun getEventManager(): EventManagerImpl = eventManager as EventManagerImpl
}
