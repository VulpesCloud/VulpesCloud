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

package org.vulpesstudios.vulpescloud.connector.bukkit

import build.buf.gen.vulpescloud.services.v1.UpdatePlayerCountRequest
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.vulpesstudios.vulpescloud.bridge.BridgeAPI
import org.vulpesstudios.vulpescloud.wrapper.Wrapper

class PlayerJoinEventListener : Listener {

    val bridgeAPI = BridgeAPI.createCoroutineAPI()

    @EventHandler
    fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        runBlocking {
            val service = bridgeAPI.getServicesAPI().getLocalService()!!
            Wrapper.instance.grpcClient.serviceAPI.updatePlayerCount(
                UpdatePlayerCountRequest.newBuilder()
                    .setPlayerCount(Bukkit.getOnlinePlayers().size)
                    .setService(service.toDefinition())
                    .build()
            )
        }
    }

    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        runBlocking {
            val service = bridgeAPI.getServicesAPI().getLocalService()!!
            Wrapper.instance.grpcClient.serviceAPI.updatePlayerCount(
                UpdatePlayerCountRequest.newBuilder()
                    .setPlayerCount(Bukkit.getOnlinePlayers().size)
                    .setService(service.toDefinition())
                    .build()
            )
        }
    }
}
