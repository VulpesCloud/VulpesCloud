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

package org.vulpesstudios.vulpescloud.bridge

import org.vulpesstudios.vulpescloud.api.players.OfflinePlayer
import org.vulpesstudios.vulpescloud.api.players.OnlinePlayer
import java.util.concurrent.CompletableFuture

interface PlayerAPI {

    interface PlayerCoroutineAPI {

        suspend fun getAllOnlinePlayers(): List<OnlinePlayer>

        suspend fun getOnlinePlayerByName(name: String): OnlinePlayer?

        suspend fun getOnlinePlayerByUUID(uuid: String): OnlinePlayer?

        suspend fun getAllRegisteredPlayers(): List<OfflinePlayer>

        suspend fun getRegisteredPlayerByName(name: String): OfflinePlayer?

        suspend fun getRegisteredPlayerByUUID(uuid: String): OfflinePlayer?

    }

    interface PlayerFutureAPI {

        fun getAllOnlinePlayers(): CompletableFuture<List<OnlinePlayer>>

        fun getOnlinePlayerByName(name: String): CompletableFuture<OnlinePlayer?>

        fun getOnlinePlayerByUUID(uuid: String): CompletableFuture<OnlinePlayer?>

        fun getAllRegisteredPlayers(): CompletableFuture<List<OfflinePlayer>>

        fun getRegisteredPlayerByName(name: String): CompletableFuture<OfflinePlayer?>

        fun getRegisteredPlayerByUUID(uuid: String): CompletableFuture<OfflinePlayer?>
    }

}
