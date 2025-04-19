package de.vulpescloud.node.utils

import de.vulpescloud.api.cluster.AuthenticationProvider
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.module.ModuleInfo
import de.vulpescloud.api.module.ModuleStates
import de.vulpescloud.api.task.Task
import de.vulpescloud.api.version.Version
import de.vulpescloud.api.version.VersionType
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

object JsonUtils : KoinComponent {

    private val authenticationProvider: AuthenticationProvider by inject()

    fun getTask(json: JSONObject): Task {
        return Task(
            json.getString("name"),
            json.getJSONArray("nodes").map { it as String },
            json.getJSONArray("templates").map { it as String },
            json.getInt("maxMemory"),
            json.getInt("maxPlayers"),
            json.getBoolean("staticServices"),
            json.getInt("minOnlineCount"),
            json.getInt("serviceCount"),
            json.getJSONArray("services").map { it as String },
            json.getBoolean("maintenance"),
            json.getInt("startPort"),
            json.getBoolean("fallback"),
            getVersion(JSONObject(json.getString("version"))),
            json.getBoolean("copyTemplateToStatic")
        )
    }

    fun getVersion(json: JSONObject): Version {
        return Version(
            json.getString("name"),
            json.getString("version"),
            VersionType.valueOf(json.getString("type")),
            json.getString("downloadURL"),
            json.getString("pluginDir"),
        )
    }

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
