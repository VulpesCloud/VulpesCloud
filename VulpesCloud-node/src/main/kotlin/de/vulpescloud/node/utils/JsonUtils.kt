package de.vulpescloud.node.utils

import de.vulpescloud.api.cluster.AuthenticationProvider
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeStates
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

object JsonUtils : KoinComponent {

    private val authenticationProvider: AuthenticationProvider by inject()

    fun getClusterNode(json: JSONObject): ClusterNode {
        return ClusterNode(
            json.getString("name"),
            UUID.fromString(json.getString("uuid")),
            json.getInt("runningServices"),
            NodeStates.valueOf(json.getString("state")),
            json.getInt("currentMemoryUsage"),
            json.getInt("maxMemoryUsage"),
            json.getString("cloudVersion"),
            json.getBoolean("headNode"),
            json.getString("hostname")
        )
    }

    fun parsePubSubMessage(string: String): JSONObject {
        val json = JSONObject(string)
        if (json.has("secret") && json.getString("secret") == authenticationProvider.getAuthenticationToken()) {
            return JSONObject(json.getString("messages"))
        } else {
            throw IllegalAccessError("trying to parse message while secret is invalid")
        }
    }

}
