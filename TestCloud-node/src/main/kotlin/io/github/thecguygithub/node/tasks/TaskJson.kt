package io.github.thecguygithub.node.tasks

import io.github.thecguygithub.api.platforms.PlatformGroupDisplay
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.platforms.Platform
import io.github.thecguygithub.node.platforms.versions.PlatformVersion
import org.json.JSONArray
import org.json.JSONObject

object TaskJson {

    fun createGroupJson(name: String, platform: Platform, version: String, maxMemory: Int, staticServices: Boolean, minOnlineCount: Int, fallbackGroup: Boolean, maintenance: Boolean, startPort: Int): JSONObject {
        val platformGroupJson = JSONObject()
        platformGroupJson.put("name", platform.id)
        platformGroupJson.put("version", version)
        platformGroupJson.put("platformType", platform.type)



        val nodesArray = mutableListOf(Node.nodeConfig?.localNode).toTypedArray()
        val templatesArray = arrayOf("every", platform.type.defaultTemplateSpace, name)

        val nodes = JSONArray()
        val templates = JSONArray()
        nodesArray.forEach { nodes.put(it) }
        templatesArray.forEach { templates.put(it) }

        val jsonTask = JSONObject()
        jsonTask.put("name", name)
        jsonTask.put("platformGroupDisplay", platformGroupJson)
        jsonTask.put("nodes", nodes)
        jsonTask.put("templates", templates)
        jsonTask.put("maxMemory", maxMemory)
        jsonTask.put("staticService", staticServices)
        jsonTask.put("minOnline", minOnlineCount)
        jsonTask.put("maintenance", maintenance)
        jsonTask.put("startPort", startPort)

        return jsonTask
    }

}