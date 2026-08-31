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

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.minimessage.MiniMessage
import org.vulpesstudios.vulpescloud.bridge.BridgeAPI
import org.vulpesstudios.vulpescloud.connector.velocity.config.getConfig
import java.util.concurrent.TimeUnit

class PlayerChooseInitialServerEventListener(
    private val bridgeAPI: BridgeAPI.BridgeFutureAPI,
    private val proxyServer: ProxyServer,
) {

    @Subscribe(order = PostOrder.FIRST)
    fun onPlayerChooseInitialServerEvent(event: PlayerChooseInitialServerEvent) {
        val services =
            bridgeAPI
                .getServicesAPI()
                .getAllServices()
                .get(5, TimeUnit.SECONDS)
                .filter {
                    it.task.software.type != org.vulpesstudios.vulpescloud.api.serversoftware.SoftwareType.PROXY &&
                        it.task.fallback
                }
                .sortedBy { it.playerCount }
        if (services.isEmpty()) {
            return
        }

        proxyServer.getServer("${services[0].task.name}-${services[0].orderedId}").ifPresent {
            event.setInitialServer(it)
        }
    }

    @Subscribe
    fun onKickedFromServerEvent(event: KickedFromServerEvent) {
        runBlocking {
            val services =
                bridgeAPI
                    .getServicesAPI()
                    .getAllServices()
                    .get(5, TimeUnit.SECONDS)
                    .filter {
                        it.task.software.type !=
                            org.vulpesstudios.vulpescloud.api.serversoftware.SoftwareType.PROXY && it.task.fallback
                    }
                    .sortedBy { it.playerCount }
            if (services.isEmpty()) {
                event.result =
                    KickedFromServerEvent.DisconnectPlayer.create(
                        MiniMessage.miniMessage()
                            .deserialize(getConfig().disconnectNoAvailableServerMessage)
                    )
            }

            if (
                event.player.currentServer.get().serverInfo.name ==
                    "${services[0].task.name}-${services[0].orderedId}"
            )
                return@runBlocking

            proxyServer.getServer("${services[0].task.name}-${services[0].orderedId}").ifPresent {
                event.result = KickedFromServerEvent.RedirectPlayer.create(it)
            }
        }
    }
}
