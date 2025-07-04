package de.vulpescloud.api.player

import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import org.json.JSONObject
import java.util.UUID

data class Player(
    val name: String,
    val uuid: UUID,
    val currentProxy: String,
    val currentServer: String
) {
    /**
     * Kicks the player from the Network.
     *
     * @param reason The reason for kicking the player. The Reason supports minimessage and Vanilla color formatting!
     */
    fun kick(reason: String) {
        getRC()?.sendMessage(
            JSONObject()
                .put("action", PlayerActions.KICK.name)
                .put("playerName", name)
                .put("reason", reason)
                .toString(),
            RedisChannels.VULPESCLOUD_ACTION_PLAYER.name
        )
    }

    /**
     * Sends a message to the player.
     *
     * @param message The message to send to the player. The Message supports minimessage and Vanilla color formatting!
     */
    fun message(message: String) {
        getRC()?.sendMessage(
            JSONObject()
                .put("action", PlayerActions.MESSAGE.name)
                .put("playerName", name)
                .put("message", message)
                .toString(),
            RedisChannels.VULPESCLOUD_ACTION_PLAYER.name
        )
    }

    /**
     * Connects the player to a specific server.
     *
     * @param serverName The name of the server to connect the player to.
     */
    fun connect(serverName: String) {
        getRC()?.sendMessage(
            JSONObject()
                .put("action", PlayerActions.CONNECT.name)
                .put("playerName", name)
                .put("serverName", serverName)
                .toString(),
            RedisChannels.VULPESCLOUD_ACTION_PLAYER.name
        )
    }
}
