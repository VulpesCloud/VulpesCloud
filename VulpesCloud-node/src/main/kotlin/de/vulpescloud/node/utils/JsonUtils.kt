package de.vulpescloud.node.utils

import de.vulpescloud.api.cluster.AuthenticationProvider
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.module.ModuleInfo
import de.vulpescloud.api.module.ModuleStates
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

    fun getModuleInfo(json: JSONObject): ModuleInfo {
        return ModuleInfo(
            json.getString("name"),
            json.getJSONArray("authors").map { it as String }.toMutableList(),
            json.getString("description"),
            json.getString("main"),
            json.getString("version"),
            json.getString("website"),
            ModuleStates.valueOf(json.getString("state"))
        )
    }

    fun parsePubSubMessage(string: String): JSONObject {
        val json = JSONObject(string)
        return if (json.has("secret") && json.getString("secret") == authenticationProvider.getAuthenticationToken()) {
            JSONObject(json.getString("message"))
        } else {
            JSONObject()
                .put("type", "INVALID_SECRET")
        }
    }

}
