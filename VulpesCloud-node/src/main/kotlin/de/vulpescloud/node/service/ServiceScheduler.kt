package de.vulpescloud.node.service

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.task.TaskProvider
import de.vulpescloud.node.Scheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

object ServiceScheduler: Scheduler(), KoinComponent {

    private val taskProvider: TaskProvider by inject()
    private val serviceProvider: ServiceProvider by inject()
    private val clusterProvider: ClusterProvider by inject()
    private val logger = LoggerFactory.getLogger(ServiceScheduler::class.java)

    override fun run() = launch {
        while (true) {
            taskProvider.tasks().forEach { task ->
                if (task.maintenance) return@forEach
                if (!task.nodes.contains(clusterProvider.localNode().name)) return@forEach

                val currentServices = serviceProvider.services().filter { it.task.name == task.name }.size
                val wantedServices = task.minOnlineCount

                if (wantedServices > currentServices) {
                    logger.info("Starting new service on task ${task.name}!")
                    val factory = (serviceProvider as ServiceProviderImpl).serviceFactories.find { it.name() == task.serviceFactoryName }

                    if (factory == null) {
                        logger.error("Tried to start service on task ${task.name}, but ServiceFactory ${task.serviceFactoryName} was not found!")
                        return@forEach
                    }

                    factory.prepareService(task).start()
                }
            }
            delay(1000)
        }
    }
}
