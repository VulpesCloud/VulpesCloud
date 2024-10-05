package io.github.thecguygithub.api.services

import io.github.thecguygithub.api.tasks.ClusterTask


interface ClusterServiceFactory {
    fun runGroupService(group: ClusterTask?)

    fun shutdownGroupService(clusterService: ClusterService?)
}