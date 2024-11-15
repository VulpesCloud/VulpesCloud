package de.vulpescloud.node.service

import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

class ServiceStartScheduler : CoroutineScope {

    companion object {
        lateinit var instance: ServiceStartScheduler
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job
    private val taskProvider = Node.taskProvider
    private val logger = LoggerFactory.getLogger(ServiceStartScheduler::class.java)

    init {
        instance = this
    }

    fun cancel() {
        job.cancel()
    }

    fun schedule() = launch {
        while (true) {
            logger.warn("Checking for services to start!")
            for (task: Task in taskProvider.tasks()!!) {
                if (task.nodes().contains(Node.nodeConfig!!.name)) continue

                if (task.minOnlineCount() < task.services()!!.size) continue

                logger.warn(task.name())
                logger.warn(task.minOnlineCount().toString())
                logger.warn(task.services()!!.size.toString())

                val serviceToStart = task.minOnlineCount() - task.services()!!.size

                logger.warn(serviceToStart.toString())

                for (i in 0 until serviceToStart) {
                    Node.serviceProvider.factory().startServiceOnTask(task)
                    delay(2000)
                }
            }
            delay(10000)
        }
    }

}