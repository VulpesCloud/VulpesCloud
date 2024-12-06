package de.vulpescloud.connector

import de.vulpescloud.wrapper.Wrapper

open class Connector {

    val wrapper = Wrapper.instance

    fun init() {
        // serviceProvider.getAllServiceFromRedis()
        // playerProvider.initializePlayerProvider()
        // playerProvider.loadPlayerDataFromRedis()

        // TimeUnit.SECONDS.sleep(1)

        // updateLocalState(ClusterServiceStates.STARTING)
    }

    fun finishStart() {
        // updateLocalState(ClusterServiceStates.ONLINE)
    }

    fun shutdownLocal() {
        // updateLocalState(ClusterServiceStates.STOPPING)
    }

}