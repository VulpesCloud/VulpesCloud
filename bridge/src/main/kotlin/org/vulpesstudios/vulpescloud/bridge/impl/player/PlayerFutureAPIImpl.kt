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
import org.vulpesstudios.vulpescloud.bridge.FutureHelper.toCompletableFuture
import org.vulpesstudios.vulpescloud.bridge.PlayerAPI
import org.vulpesstudios.vulpescloud.wrapper.Wrapper
import java.util.concurrent.CompletableFuture

class PlayerFutureAPIImpl : PlayerAPI.PlayerFutureAPI {
    private val playerStub = Wrapper.instance.grpcClient.futurePlayerAPI

    override fun getAllOnlinePlayers(): CompletableFuture<List<OnlinePlayer>> {
        return playerStub
            .getAllOnlinePlayers(getAllOnlinePlayersRequest {})
            .toCompletableFuture()
            .thenApply { player -> player.onlinePlayersList.map { it.toAPI() } }
    }

    override fun getOnlinePlayerByUUID(uuid: String): CompletableFuture<OnlinePlayer?> {
        return getAllOnlinePlayers().thenApply { it.find { player -> player.uuid == uuid } }
    }

    override fun getOnlinePlayerByName(name: String): CompletableFuture<OnlinePlayer?> {
        return getAllOnlinePlayers().thenApply { it.find { player -> player.name == name } }
    }

    override fun getAllRegisteredPlayers(): CompletableFuture<List<OfflinePlayer>> {
        return playerStub
            .getAllOfflinePlayers(getOfflinePlayersRequest {})
            .toCompletableFuture()
            .thenApply { player -> player.offlinePlayersList.map { it.toAPI() } }
    }

    override fun getRegisteredPlayerByName(name: String): CompletableFuture<OfflinePlayer?> {
        return getAllRegisteredPlayers().thenApply { it.find { player -> player.name == name } }
    }

    override fun getRegisteredPlayerByUUID(uuid: String): CompletableFuture<OfflinePlayer?> {
        return getAllRegisteredPlayers().thenApply { it.find { player -> player.uuid == uuid } }
    }
}
