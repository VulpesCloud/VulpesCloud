package de.vulpescloud.api.players

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
