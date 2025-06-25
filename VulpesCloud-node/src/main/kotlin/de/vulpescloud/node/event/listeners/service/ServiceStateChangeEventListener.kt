package de.vulpescloud.node.event.listeners.service

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.event.EventListener
import de.vulpescloud.api.event.events.service.ServiceStateChangeEvent
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.task.TaskProvider
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import org.json.JSONObject
import org.slf4j.LoggerFactory

class ServiceStateChangeEventListener(
    private val serviceProvider: ServiceProvider,
    private val taskProvider: TaskProvider,
    private val clusterProvider: ClusterProvider,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onServiceStateChangeEvent(event: ServiceStateChangeEvent) {
        logger.info("Service &m${event.serviceInfo.name} &7is now &e${event.newState}")

        val servicesOnTask =
            serviceProvider.services().filter { it.task.name == event.serviceInfo.task.name }
        val servicesOnNode =
            serviceProvider.services().filter {
                it.runningNode.name == clusterProvider.localNode().name
            }

        val task = taskProvider.getTaskByName(event.serviceInfo.task.name)
        if (task == null) {
            logger.error("Received ServiceStateChangeEvent for a task that is not registered!")
            return
        }
        task.copy(serviceCount = servicesOnTask.size, services = servicesOnTask).let {
            taskProvider.updateTask(it)
        }

        clusterProvider.localNode().copy(runningServices = servicesOnNode.size).let {
            getRC()?.setHashField("VULPESCLOUD:NODES", it.name, JSONObject(it).toString())
        }
    }
}
