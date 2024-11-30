/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.connector

import de.vulpescloud.api.network.redis.RedisHashNames
import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.api.services.ClusterServiceStates
import de.vulpescloud.api.services.ServiceMessageBuilder
import de.vulpescloud.api.services.builder.ServiceEventMessageBuilder
import de.vulpescloud.bridge.player.PlayerProvider
import de.vulpescloud.bridge.service.ServiceProvider
import de.vulpescloud.bridge.service.impl.ServiceImpl
import de.vulpescloud.wrapper.Wrapper
import org.json.JSONObject
import java.util.concurrent.TimeUnit

open class Connector {

    val serviceProvider = ServiceProvider()
    val playerProvider = PlayerProvider()

    val wrapper = Wrapper.instance

    fun init() {
        serviceProvider.getAllServiceFromRedis()
        playerProvider.initializePlayerProvider()
        playerProvider.loadPlayerDataFromRedis()

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
        wrapper.getRC()?.sendMessage(
            ServiceMessageBuilder.registrationMessageBuilder().serviceRegisterBuilder()
                .setService(serviceProvider.getLocalService())
                .build(),
            RedisPubSubChannels.VULPESCLOUD_SERVICE_REGISTER.name
        )
    }

    fun unregisterLocalService() {
        wrapper.getRC()?.sendMessage(
            ServiceMessageBuilder.registrationMessageBuilder().serviceUnregisterBuilder()
                .setService(serviceProvider.getLocalService())
                .build(),
            RedisPubSubChannels.VULPESCLOUD_SERVICE_UNREGISTER.name
        )
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
            RedisPubSubChannels.VULPESCLOUD_SERVICE_EVENT.name
        )

        wrapper.getRC()?.setHashField(RedisHashNames.VULPESCLOUD_SERVICES.name, ls.name(), JSONObject(si).toString())

        serviceProvider.getAllServiceFromRedis()
    }

}