package de.vulpescloud.api.service

import de.vulpescloud.api.Named
import de.vulpescloud.api.task.Task

interface ServiceFactory : Named {

    fun prepareService(task: Task): AbstractService

}