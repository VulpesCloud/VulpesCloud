package de.vulpescloud.node.json

import de.vulpescloud.api.player.VulpesPlayer
import de.vulpescloud.node.Node
import de.vulpescloud.node.json.ServiceSerializer.serviceFromJson
import de.vulpescloud.node.player.VulpesPlayerImpl
import org.json.JSONObject
import java.util.UUID

object PlayerSerializer {

    fun playerFromJson(json: JSONObject): VulpesPlayer {
        return VulpesPlayerImpl(
            json.getString("name"),
            UUID.fromString(json.getString("uuid")),
            if (json.has("currentServer")) serviceFromJson(json.getJSONObject("currentServer")) else null,
            if (json.has("currentProxy")) serviceFromJson(json.getJSONObject("currentProxy")) else null
        )
    }

    fun jsonFromPlayer(player: VulpesPlayerImpl): JSONObject {
        return JSONObject(player)
    }

}