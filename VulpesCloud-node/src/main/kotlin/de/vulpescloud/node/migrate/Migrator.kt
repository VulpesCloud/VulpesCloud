package de.vulpescloud.node.migrate

import de.vulpescloud.api.task.Task
import de.vulpescloud.api.template.Template
import de.vulpescloud.api.version.SingleVersion
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.utils.JsonUtils.getService
import de.vulpescloud.node.utils.JsonUtils.getSingleVersion
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory

class Migrator {
    private val logger = LoggerFactory.getLogger(Migrator::class.java)

    fun migrateV1ToV2() {
        logger.info("Starting migration from V1 to V2!")
        val tasks = Tools.getAllV1Tasks()
        val newTasks =
            tasks.map { t ->
                val json = JSONObject(t.toString())
                Task(
                    json.getString("name"),
                    json.getJSONArray("nodes").map { it as String },
                    listOf(Template(json.getString("name"), "local")),
                    json.getInt("maxMemory"),
                    json.getInt("maxPlayers"),
                    json.getBoolean("staticServices"),
                    json.getInt("minOnlineCount"),
                    0,
                    emptyList(),
                    json.getBoolean("maintenance"),
                    json.getInt("startPort"),
                    json.getBoolean("fallback"),
                    Tools.getSingleVersion(json.getJSONObject("version")),
                    json.getBoolean("copyTemplateToStatic"),
                    json.getString("serviceFactoryName"),
                    json.getJSONArray("environmentVars").map {
                        it as JSONObject
                        it.getString("first") to it.getString("second")
                    } as MutableList<Pair<String, String>>,
                )
            }
    }

    private object Tools {
        fun getAllV1Tasks(): JSONArray {
            val tasks = JSONArray()
            getRC()?.getAllHashValues("VULPESCLOUD_TASKS")?.forEach { tasks.put(JSONObject(it)) }
            return tasks
        }

        fun getSingleVersion(json: JSONObject): SingleVersion {
            return SingleVersion(
                json.getString("environment"),
                json.getString("version"),
                json.getString("downloadURL"),
                json.getString("pluginDir"),
                VersionType.valueOf(json.getString("versionType")),
            )
        }
    }
}
