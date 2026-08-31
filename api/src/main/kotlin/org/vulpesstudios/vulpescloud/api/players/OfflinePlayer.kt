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

package org.vulpesstudios.vulpescloud.api.players

import build.buf.gen.vulpescloud.players.v1.OfflinePlayer
import build.buf.gen.vulpescloud.players.v1.offlinePlayer
import kotlinx.serialization.Serializable

@Serializable
data class OfflinePlayer(
    val name: String,
    val uuid: String,
    val lastSeen: Long,
    val firstSeen: Long,
) {
    fun toProto(): OfflinePlayer = offlinePlayer {
        uuid = this@OfflinePlayer.uuid
        name = this@OfflinePlayer.name
        lastSeen = this@OfflinePlayer.lastSeen
        firstSeen = this@OfflinePlayer.firstSeen
    }
}

fun OfflinePlayer.toAPI() =
    OfflinePlayer(name, uuid, lastSeen, firstSeen)
