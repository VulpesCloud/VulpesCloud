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

package org.vulpesstudios.vulpescloud.node.players

import build.buf.gen.vulpescloud.events.v1.PlayerJoinEvent
import build.buf.gen.vulpescloud.events.v1.PlayerQuitEvent
import build.buf.gen.vulpescloud.events.v1.PlayerSwitchServerEvent
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.players.OfflinePlayer
import org.vulpesstudios.vulpescloud.api.players.toAPI
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.event.EventsService
import org.vulpesstudios.vulpescloud.node.utils.PropertyUtils.isLoggingPlayerEvents
import java.util.concurrent.CopyOnWriteArrayList

object PlayerJoinEventListener {
    private var job: Job? = null
    private val logger = LoggerFactory.getLogger(PlayerJoinEventListener::class.java)
    private val offlinePlayerDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("offlinePlayers")
    }

    fun subscribe() {
        job =
            EventsService.subscribe<PlayerJoinEvent> { event ->
                val proxyName = event.player.proxyServiceName
                if (isLoggingPlayerEvents()) {
                    logger.info(
                        "Player <aqua>${event.player.name}</aqua> <gray>joined the network on proxy</gray> <white>$proxyName</white>"
                    )
                }
                if (Node.instance.nodeServices.any { it.service.name() == proxyName }) {
                    Node.instance.nodeProxyPlayers
                        .getOrPut(proxyName, ::CopyOnWriteArrayList)
                        .add(event.player.toAPI())
                }

                if (offlinePlayerDatabase.get(event.player.uuid) == null) {
                    if (isLoggingPlayerEvents()) {
                        logger.info(
                            "Player <aqua>${event.player.name}</aqua> <gray>joined the network for the first time!</gray>"
                        )
                    }
                    offlinePlayerDatabase.insertIgnore(
                        event.player.uuid,
                        Json.encodeToJsonElement(
                            OfflinePlayer(
                                event.player.name,
                                event.player.uuid,
                                System.currentTimeMillis(),
                                System.currentTimeMillis(),
                            )
                        ),
                    )
                }
            }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}

object PlayerQuitEventListener {
    private var job: Job? = null
    private val logger = LoggerFactory.getLogger(PlayerQuitEventListener::class.java)
    private val offlinePlayerDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("offlinePlayers")
    }

    fun subscribe() {
        job =
            EventsService.subscribe<PlayerQuitEvent> { event ->
                val proxyName = event.lastProxyName
                if (isLoggingPlayerEvents()) {
                    logger.info("Player <aqua>${event.player.name}</aqua> <gray>left the network!</gray>")
                }
                if (Node.instance.nodeServices.any { it.service.name() == proxyName }) {
                    Node.instance.nodeProxyPlayers
                        .getOrPut(proxyName, ::CopyOnWriteArrayList)
                        .removeIf { it.uuid == event.player.uuid }
                }

                offlinePlayerDatabase.upsert(
                    event.player.uuid,
                    Json.encodeToJsonElement(event.player.toAPI()),
                )
            }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}

object PlayerSwitchServerEventListener {
    private var job: Job? = null
    private val logger = LoggerFactory.getLogger(PlayerSwitchServerEventListener::class.java)

    fun subscribe() {
        job =
            EventsService.subscribe<PlayerSwitchServerEvent> { event ->
                val oldServerName = event.oldServer
                val newServerName = event.newServer
                if (isLoggingPlayerEvents()) {
                    logger.info(
                        "Player <aqua>${event.player.name}</aqua> <gray>switched servers from</gray> <white>$oldServerName</white> <gray>to</gray> <white>$newServerName</white><gray>!</gray>"
                    )
                }
                if (Node.instance.nodeServices.any { it.service.name() == oldServerName }) {
                    Node.instance.nodeServerPlayers
                        .getOrPut(oldServerName, ::CopyOnWriteArrayList)
                        .removeIf { it.uuid == event.player.uuid }
                }
                if (Node.instance.nodeServices.any { it.service.name() == newServerName }) {
                    Node.instance.nodeServerPlayers
                        .getOrPut(newServerName, ::CopyOnWriteArrayList)
                        .add(event.player.toAPI())
                }

                if (
                    Node.instance.nodeServices.any {
                        it.service.name() == event.player.proxyServiceName
                    }
                ) {
                    Node.instance.nodeProxyPlayers
                        .getOrPut(event.player.proxyServiceName, ::CopyOnWriteArrayList)
                        .apply {
                            removeIf { it.uuid == event.player.uuid }
                            add(event.player.toAPI())
                        }
                }
            }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}
