package io.github.thecguygithub.api.services

import io.github.thecguygithub.api.tasks.ClusterTask


interface ClusterServiceFactory {
    fun runGroupService(task: ClusterTask)

    fun shutdownGroupService(clusterService: ClusterService)
}