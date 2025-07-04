package de.vulpescloud.bridge

import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.task.TaskProvider
import de.vulpescloud.bridge.event.EventManagerImpl
import de.vulpescloud.bridge.service.ServiceProviderImpl
import de.vulpescloud.bridge.task.TaskProviderImpl

object VulpesBridge {

    private val serviceProvider: ServiceProvider = ServiceProviderImpl()
    private val eventManager: EventManager = EventManagerImpl()
    private val taskProvider: TaskProvider = TaskProviderImpl()

    fun getServiceProvider(): ServiceProviderImpl = serviceProvider as ServiceProviderImpl

    fun getEventManager(): EventManagerImpl = eventManager as EventManagerImpl

    fun getTaskProvider(): TaskProviderImpl = taskProvider as TaskProviderImpl
}
