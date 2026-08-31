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

package org.vulpesstudios.vulpescloud.connector.velocity.commands

import com.velocitypowered.api.proxy.ProxyServer
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.minimessage.MiniMessage
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.bridge.BridgeAPI
import org.vulpesstudios.vulpescloud.connector.velocity.config.getConfig
import java.util.concurrent.TimeUnit

class HubCommand(proxyServer: ProxyServer, bridgeAPI: BridgeAPI.BridgeFutureAPI) {

    private val logger = LoggerFactory.getLogger("HubCommand")
    private val miniMessage = MiniMessage.miniMessage()

    val command =
        CommandTree("hub")
            .withAliases("lobby", "l", "leave")
            .executesPlayer(
                PlayerCommandExecutor { sender, _ ->
                    val fallbackServer =
                        bridgeAPI
                            .getServicesAPI()
                            .getAllServices()
                            .get(5, TimeUnit.SECONDS)
                            .filter { it.task.fallback }

                    if (fallbackServer.isEmpty()) {
                        logger.error("No fallback server found!")
                        runBlocking {
                            sender.sendMessage(
                                miniMessage.deserialize(
                                    getConfig().disconnectNoAvailableServerMessage
                                )
                            )
                        }
                        return@PlayerCommandExecutor
                    }

                    sender
                        .createConnectionRequest(
                            proxyServer
                                .getServer(
                                    "${fallbackServer[0].task.name}-${fallbackServer[0].orderedId}"
                                )
                                .get()
                        )
                        .connectWithIndication()
                }
            )
}
