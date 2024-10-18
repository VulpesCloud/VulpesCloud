package io.github.thecguygithub.api.services

import io.github.thecguygithub.api.tasks.ClusterTask


interface ClusterServiceFactory {

    fun startServiceOnTask(task: ClusterTask)

    fun shutdownService(clusterService: ClusterService)

    fun shutdownAllServicesOnTask(task: ClusterTask)

}