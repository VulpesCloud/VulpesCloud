package de.vulpescloud.node.utils

import de.vulpescloud.api.cluster.AuthenticationProvider
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.module.ModuleInfo
import de.vulpescloud.api.module.ModuleStates
import de.vulpescloud.api.player.Player
import de.vulpescloud.api.service.Service
import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.api.task.Task
import de.vulpescloud.api.template.Template
import de.vulpescloud.api.version.SingleVersion
import de.vulpescloud.api.version.Version
import de.vulpescloud.api.version.VersionType
import java.util.*
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object JsonUtils : KoinComponent {

    private val authenticationProvider: AuthenticationProvider by inject()

    fun getService(json: JSONObject): Service {
        return Service(
            getTask(json.getJSONObject("task")),
            UUID.fromString(json.getString("uuid")),
            json.getInt("orderedId"),
            json.getInt("port"),
            getClusterNode(json.getJSONObject("runningNode")),
            ServiceStates.valueOf(json.getString("state")),
            json.getInt("maxPlayers"),
            json.getInt("onlinePlayerCount"),
            json.getString("name"),
            json.getJSONArray("onlinePlayers").map {
                val player = it as JSONObject
                getPlayer(player)
            },
            json.getJSONArray("environmentVars").map {
                val pair = it as JSONObject
                pair.getString("key") to pair.getString("value")
            },
        )
    }

    fun getPlayer(json: JSONObject): Player {
        return Player(
            json.getString("name"),
            UUID.fromString(json.getString("uuid")),
            json.getString("currentProxy"),
            json.getString("currentServer"),
        )
    }

    fun getTask(json: JSONObject): Task {
        return Task(
            json.getString("name"),
            json.getJSONArray("nodes").map { it as String },
            json.getJSONArray("templates").map {
                val jsonTemplate = it as JSONObject
                getTemplate(jsonTemplate)
            },
            json.getInt("maxMemory"),
            json.getInt("maxPlayers"),
            json.getBoolean("staticServices"),
            json.getInt("minOnlineCount"),
            json.getInt("serviceCount"),
            json.getJSONArray("services").map {
                val serviceJson = it as JSONObject
                getService(serviceJson)
            },
            json.getBoolean("maintenance"),
            json.getInt("startPort"),
            json.getBoolean("fallback"),
            getSingleVersion(json.getJSONObject("version")),
            json.getBoolean("copyTemplateToStatic"),
        )
    }

    fun getTemplate(json: JSONObject): Template {
        println(json)
        return Template(
            json.getString("name"),
            json.getString("storage"),
        )
    }

    fun getVersion(json: JSONObject): Version {
        val version = mutableListOf<SingleVersion>()
        val array = json.getJSONArray("versions")

        for (i in 0 until array.length()) {
            val versionObject = array.getJSONObject(i)
            version.add(getSingleVersion(versionObject))
        }

        return Version(
            json.getString("name"),
            VersionType.valueOf(json.getString("type")),
            json.getString("pluginDir"),
            version,
        )
    }

    fun getSingleVersion(json: JSONObject): SingleVersion {
        return SingleVersion(
            json.getString("name"),
            json.getString("version"),
            json.getString("downloadURL"),
            json.getString("pluginDir"),
            VersionType.valueOf(json.getString("type"))
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
            json.getString("hostname"),
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
            ModuleStates.valueOf(json.getString("state")),
        )
    }

    fun parsePubSubMessage(string: String): JSONObject {
        val json = JSONObject(string)
        return if (
            json.has("secret") &&
                json.getString("secret") == authenticationProvider.getAuthenticationToken()
        ) {
            JSONObject(json.getString("message"))
        } else {
            JSONObject().put("type", "INVALID_SECRET")
        }
    }
}
