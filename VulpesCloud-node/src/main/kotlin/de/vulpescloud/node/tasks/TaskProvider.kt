package de.vulpescloud.node.tasks

import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.node.Node
import org.json.JSONObject

class TaskProvider {

    fun tasks(): List<Task> {
        val taskList = mutableListOf<Task>()
        val list = Node.instance.getRC()?.getAllHashValues(RedisHashNames.VULPESCLOUD_TASKS.name)
        if (!list.isNullOrEmpty()) {
            list.forEach {
                val task = taskFromJson(JSONObject(it))
                taskList.add(task)
            }
        }

        return taskList
    }

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