package de.vulpescloud.api.services

import de.vulpescloud.api.tasks.Task


interface ClusterServiceFactory {

    fun startServiceOnTask(task: Task)

    fun shutdownService(clusterService: ClusterService)

    fun shutdownAllServicesOnTask(task: Task)

}