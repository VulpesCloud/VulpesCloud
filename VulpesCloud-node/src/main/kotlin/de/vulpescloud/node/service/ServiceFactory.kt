package de.vulpescloud.node.service

import de.vulpescloud.api.service.Service
import de.vulpescloud.api.task.Task

interface ServiceFactory {

    fun prepareService(task: Task): Pair<Service, LocalService>

}
