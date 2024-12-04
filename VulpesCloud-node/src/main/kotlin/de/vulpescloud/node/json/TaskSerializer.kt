package de.vulpescloud.node.json

import de.vulpescloud.api.tasks.Task
import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.node.tasks.TaskImpl
import org.json.JSONObject

object TaskSerializer {

    fun taskFromJson(json: JSONObject): Task {
        val jsonVersion = json.getJSONObject("version")
        val version = VersionInfo(
            jsonVersion.getString("environment"),
            VersionType.valueOf(jsonVersion.getString("versionType")),
            jsonVersion.getString("version")
        )

        return TaskImpl(
            json.getString("name"),
            json.getInt("maxMemory"),
            version,
            json.getJSONArray("templates").toList().map { it as String },
            json.getJSONArray("nodes").toList().map { it as String },
            json.getInt("maxPlayers"),
            json.getBoolean("staticServices"),
            json.getInt("minOnlineCount"),
            json.getBoolean("maintenance"),
            json.getInt("startPort"),
            json.getBoolean("fallback")
        )
    }

    fun jsonFromTask(task: Task): JSONObject {
        return JSONObject(task)
    }

}