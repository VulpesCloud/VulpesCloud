package de.vulpescloud.node.service

import de.vulpescloud.api.task.TaskProvider
import de.vulpescloud.node.Scheduler
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

object ServiceScheduler: Scheduler(), KoinComponent {

    private val taskProvider: TaskProvider by inject()
    private val serviceFactory: ServiceFactory by inject()
    private val logger = LoggerFactory.getLogger(ServiceScheduler::class.java)

    override fun run() = launch {

    }
}
