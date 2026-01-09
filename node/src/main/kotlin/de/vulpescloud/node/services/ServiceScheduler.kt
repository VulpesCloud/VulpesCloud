package de.vulpescloud.node.services

import build.buf.gen.vulpescloud.services.v1.getAllServicesRequest
import build.buf.gen.vulpescloud.tasks.v1.getAllTasksRequest
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

object ServiceScheduler {

    private var job: Job? = null
    private val logger = LoggerFactory.getLogger("ServiceScheduler")

    fun start() {
        job =
            NodeCoroutineScope.launch {
                while (true) {
                    val tasks =
                        Node.instance.localGrpcClient.tasksAPI
                            .getAllTasks(getAllTasksRequest {})
                            .tasksList
                            .map { Task.fromDefinition(it) }

                    val services =
                        Node.instance.localGrpcClient.serviceAPI
                            .getAllServices(getAllServicesRequest {})
                            .servicesList
                            .map { Service.fromDefinition(it) }

                    tasks.forEach { task ->
                        logger.debug("Checking task ${task.name}")
                        val currentServiceCount = services.count { it.task.name == task.name }
                        if (task.maintenance) return@forEach
                        if (task.preferredNode != Node.instance.configProvider.config.nodeName)
                            return@forEach
                        if (task.minOnlineServices <= currentServiceCount) return@forEach
                        logger.info("Starting service on Task ${task.name}")

                        val factory =
                            Node.instance.serviceFactoryProvider.findServiceFactory(
                                task.serviceFactoryName
                            )
                                ?: throw IllegalArgumentException(
                                    "Unable to find ServiceFactory ${task.serviceFactoryName}"
                                )
                        factory.prepareService(task).start()
                    }

                    delay(5.seconds)
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
