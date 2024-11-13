package de.vulpescloud.connector

import de.vulpescloud.api.network.redis.RedisHashNames
import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.api.services.ClusterServiceStates
import de.vulpescloud.api.services.builder.ServiceEventMessageBuilder
import de.vulpescloud.bridge.service.ServiceProvider
import de.vulpescloud.bridge.service.impl.ServiceImpl
import de.vulpescloud.wrapper.Wrapper
import org.json.JSONObject
import java.util.concurrent.TimeUnit

open class Connector {

    val serviceProvider = ServiceProvider()

    val wrapper = Wrapper.instance

    fun init() {
        serviceProvider.getAllServiceFromRedis()

        TimeUnit.SECONDS.sleep(1)

        updateLocalState(ClusterServiceStates.STARTING)
    }

    fun finishStart() {
        updateLocalState(ClusterServiceStates.ONLINE)
    }

    fun shutdownLocal() {
        updateLocalState(ClusterServiceStates.STOPPING)
    }

    fun registerLocalService() {

    }

    fun unregisterLocalService() {

    }

    private fun updateLocalState(state: ClusterServiceStates) {
        val ls = serviceProvider.getLocalService()
        val si = ServiceImpl(
            ls.task(),
            ls.orderedId(),
            ls.id()!!,
            ls.port(),
            ls.hostname()!!,
            ls.runningNode()!!,
            ls.maxPlayers(),
            state
        )

        wrapper.getRC()?.sendMessage(
            ServiceEventMessageBuilder.stateEventBuilder()
                .setService(ls)
                .setState(state)
                .build(),
            RedisPubSubChannels.VULPESCLOUD_EVENT_SERVICE.name
        )

        wrapper.getRC()?.setHashField(RedisHashNames.VULPESCLOUD_SERVICES.name, ls.name(), JSONObject(si).toString())

        serviceProvider.getAllServiceFromRedis()
    }

}