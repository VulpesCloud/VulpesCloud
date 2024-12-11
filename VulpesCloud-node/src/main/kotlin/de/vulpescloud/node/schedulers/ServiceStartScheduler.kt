package de.vulpescloud.node.schedulers

import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.services.ServiceFactory
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

object ServiceStartScheduler : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job
    private val logger = LoggerFactory.getLogger(ServiceStartScheduler::class.java)

    fun cancel() {
        job.cancel()
    }

    fun schedule() = launch {
        while (true) {
            for (task: Task in Node.instance.taskProvider.tasks()) {
                if (task.maintenance()) continue
                if (!task.nodes().contains(Node.instance.config.name)) continue
                if (task.minOnlineCount() < task.services()!!.size) continue
                val serviceToStart = task.minOnlineCount() - task.services()!!.size

                for (i in 0 until serviceToStart) {
                    ServiceFactory.prepareStartedService(task)
                }
            }
            delay(10000)
        }
    }

}