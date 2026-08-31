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

package org.vulpesstudios.vulpescloud.connector.velocity.events

import build.buf.gen.vulpescloud.events.v1.PlayerActionEvent
import build.buf.gen.vulpescloud.events.v1.PlayerActions
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.bridge.BridgeAPI

class VelocityPlayerActionEventListener(
    proxyServer: ProxyServer,
    bridgeAPI: BridgeAPI.BridgeCoroutineAPI,
) {
    private val minimessage = MiniMessage.miniMessage()
    private val logger = LoggerFactory.getLogger(VelocityPlayerActionEventListener::class.java)

    init {
        bridgeAPI.getEventAPI().subscribe<PlayerActionEvent> { event ->
            if (proxyServer.getPlayer(event.player.name).isPresent) {
                when (event.action) {
                    PlayerActions.KICK -> {
                        val reason = event.dataMap["reason"]
                        proxyServer
                            .getPlayer(event.player.name)
                            .get()
                            .disconnect(minimessage.deserialize(reason.toString()))
                    }
                    PlayerActions.MESSAGE -> {
                        val message = event.dataMap["message"]
                        proxyServer
                            .getPlayer(event.player.name)
                            .get()
                            .sendMessage(minimessage.deserialize(message.toString()))
                    }
                    PlayerActions.TITLE -> {
                        val title = minimessage.deserialize(event.dataMap["title"].toString())
                        val subtitle = minimessage.deserialize(event.dataMap["subtitle"].toString())
                        val finalTitle = Title.title(title, subtitle)
                        proxyServer.getPlayer(event.player.name).get().showTitle(finalTitle)
                    }
                    PlayerActions.ACTION_BAR -> {
                        val message = event.dataMap["message"]
                        proxyServer
                            .getPlayer(event.player.name)
                            .get()
                            .sendActionBar(minimessage.deserialize(message.toString()))
                    }
                    PlayerActions.CONNECT -> {
                        val targetServer = event.dataMap["targetServer"]
                        proxyServer
                            .getPlayer(event.player.name)
                            .get()
                            .createConnectionRequest(proxyServer.getServer(targetServer).get())
                            .connectWithIndication()
                    }
                    PlayerActions.UNRECOGNIZED -> logger.error("Player Action is unrecognized!")
                    PlayerActions.UNSPECIFIED -> logger.error("Player Action is unspecified!")
                }
            }
        }
    }
}
