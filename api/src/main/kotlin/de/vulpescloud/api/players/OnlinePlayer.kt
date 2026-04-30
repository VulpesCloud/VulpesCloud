package de.vulpescloud.api.players

import build.buf.gen.vulpescloud.players.v1.OnlinePlayer
import build.buf.gen.vulpescloud.players.v1.onlinePlayer

data class OnlinePlayer(
    val name: String,
    val uuid: String,
    val proxyServiceName: String,
    val serverServiceName: String,
) {
    fun toProto(): OnlinePlayer = onlinePlayer {
        name = this@OnlinePlayer.name
        uuid = this@OnlinePlayer.uuid
        proxyServiceName = this@OnlinePlayer.proxyServiceName
        serverServiceName = this@OnlinePlayer.serverServiceName
    }
}

fun OnlinePlayer.toAPI() = OnlinePlayer(name, uuid, proxyServiceName, serverServiceName)
