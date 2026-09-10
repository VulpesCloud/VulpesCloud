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

package org.vulpesstudios.vulpescloud.bridge.impl.player

import build.buf.gen.vulpescloud.players.v1.getAllOnlinePlayersRequest
import build.buf.gen.vulpescloud.players.v1.getOfflinePlayersRequest
import org.vulpesstudios.vulpescloud.api.players.OfflinePlayer
import org.vulpesstudios.vulpescloud.api.players.OnlinePlayer
import org.vulpesstudios.vulpescloud.api.players.toAPI
import org.vulpesstudios.vulpescloud.bridge.PlayerAPI
import org.vulpesstudios.vulpescloud.wrapper.Wrapper

class PlayerCoroutineAPIImpl : PlayerAPI.PlayerCoroutineAPI {

    private val playerStub = Wrapper.instance.grpcClient.playerAPI

    override suspend fun getAllOnlinePlayers(): List<OnlinePlayer> {
        return playerStub.getAllOnlinePlayers(getAllOnlinePlayersRequest {}).onlinePlayersList.map {
            it.toAPI()
        }
    }

    override suspend fun getOnlinePlayerByName(name: String): OnlinePlayer? {
        return getAllOnlinePlayers().find { it.name == name }
    }

    override suspend fun getOnlinePlayerByUUID(uuid: String): OnlinePlayer? {
        return getAllOnlinePlayers().find { it.uuid == uuid }
    }

    override suspend fun getAllRegisteredPlayers(): List<OfflinePlayer> {
        return playerStub.getAllOfflinePlayers(getOfflinePlayersRequest {}).offlinePlayersList.map {
            it.toAPI()
        }
    }

    override suspend fun getRegisteredPlayerByName(name: String): OfflinePlayer? {
        return getAllRegisteredPlayers().find { it.name == name }
    }

    override suspend fun getRegisteredPlayerByUUID(uuid: String): OfflinePlayer? {
        return getAllRegisteredPlayers().find { it.uuid == uuid }
    }
}
