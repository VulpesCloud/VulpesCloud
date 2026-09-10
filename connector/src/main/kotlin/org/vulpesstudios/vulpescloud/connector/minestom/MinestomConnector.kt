/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.connector.minestom

import build.buf.gen.vulpescloud.events.v1.serviceStateChangedEvent
import build.buf.gen.vulpescloud.services.v1.ServiceState
import build.buf.gen.vulpescloud.services.v1.updatePlayerCountRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minestom.server.MinecraftServer
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.bridge.BridgeAPI
import org.vulpesstudios.vulpescloud.wrapper.Wrapper

class MinestomConnector private constructor(val debugLogging: Boolean) {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val bridgeAPI = BridgeAPI.createCoroutineAPI()
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val eventNode = EventNode.all("minestom-connector-vulpescloud")
    companion object {
        private lateinit var connector: MinestomConnector

        fun initialize(debugLogging: Boolean = false): MinestomConnector {
            this.connector = MinestomConnector(debugLogging)
            return connector
        }
    }

    init {
        logger.info("Initializing MinestomConnector...")
        log("Adding Listeners to EventNode!")
        eventNode.addListener(PlayerSpawnEvent::class.java) { _ ->
            updatePlayerCount()
        }
        eventNode.addListener(PlayerDisconnectEvent::class.java) { _ ->
            updatePlayerCount()
        }
        log("Registering EventNode!")
        MinecraftServer.getGlobalEventHandler().addChild(eventNode)
        logger.info("MinestomConnector initialized!")
    }

    fun markServiceRunning() {
        log("Publishing ServiceStateChangedEvent!")
        coroutineScope.launch {
            val service = bridgeAPI.getServicesAPI().getLocalService()!!
            bridgeAPI
                .getEventAPI()
                .publish(
                    serviceStateChangedEvent {
                        this.service = service.toDefinition()
                        this.newState = ServiceState.SERVICE_STATE_RUNNING
                        this.oldState = service.state.toServiceState()
                    },
                    true,
                )
        }
    }

    fun shutdown() {
        logger.info("Shutting down MinestomConnector...")
        MinecraftServer.getGlobalEventHandler().removeChild(eventNode)
        coroutineScope.launch {
            val service = bridgeAPI.getServicesAPI().getLocalService()!!
            bridgeAPI
                .getEventAPI()
                .publish(
                    serviceStateChangedEvent {
                        this.service = service.toDefinition()
                        this.newState = ServiceState.SERVICE_STATE_STOPPED
                        this.oldState = service.state.toServiceState()
                    },
                    true,
                )
        }
        logger.info("MinestomConnector shut down!")
    }

    private fun updatePlayerCount() {
        coroutineScope.launch {
            val service = bridgeAPI.getServicesAPI().getLocalService()!!
            Wrapper.instance.grpcClient.serviceAPI.updatePlayerCount(updatePlayerCountRequest {
                this.service = service.toDefinition()
                this.playerCount = MinecraftServer.getConnectionManager().onlinePlayerCount
            })
        }
    }

    private fun log(message: String) {
        if (debugLogging) {
            logger.info(message)
        }
    }
}
