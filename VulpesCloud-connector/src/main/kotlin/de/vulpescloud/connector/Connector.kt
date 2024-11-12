package de.vulpescloud.connector

import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.api.services.ClusterServiceStates
import de.vulpescloud.api.services.builder.ServiceEventMessageBuilder
import de.vulpescloud.bridge.service.ServiceProvider
import de.vulpescloud.wrapper.Wrapper
import java.util.concurrent.TimeUnit

open class Connector {

    val serviceProvider = ServiceProvider()

    val wrapper = Wrapper.instance

    fun init() {
        serviceProvider.getAllServiceFromRedis()

        TimeUnit.SECONDS.sleep(1)

        wrapper.getRC()?.sendMessage(
            ServiceEventMessageBuilder.stateEventBuilder()
                .setService(serviceProvider.getLocalService())
                .setState(ClusterServiceStates.STARTING)
                .build(),
            RedisPubSubChannels.VULPESCLOUD_EVENT_SERVICE.name
        )

        serviceProvider.getLocalService().state()

        serviceProvider.getLocalService().update()
    }

    fun finishStart() {
        wrapper.getRC()?.sendMessage(
            ServiceEventMessageBuilder.stateEventBuilder()
                .setService(serviceProvider.getLocalService())
                .setState(ClusterServiceStates.ONLINE)
                .build(),
            RedisPubSubChannels.VULPESCLOUD_EVENT_SERVICE.name
        )
        serviceProvider.getLocalService().update()
    }

    fun shutdownLocal() {
        wrapper.getRC()?.sendMessage(
            ServiceEventMessageBuilder.stateEventBuilder()
                .setService(serviceProvider.getLocalService())
                .setState(ClusterServiceStates.STOPPING)
                .build(),
            RedisPubSubChannels.VULPESCLOUD_EVENT_SERVICE.name
        )
        serviceProvider.getLocalService().update()
    }

}