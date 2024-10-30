package io.github.thecguygithub.api.services

import io.github.thecguygithub.api.tasks.Task


interface ClusterServiceFactory {

    fun startServiceOnTask(task: Task)

    fun shutdownService(clusterService: ClusterService)

    fun shutdownAllServicesOnTask(task: Task)

}