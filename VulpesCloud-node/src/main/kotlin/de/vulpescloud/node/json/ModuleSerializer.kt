package de.vulpescloud.node.json

import de.vulpescloud.api.modules.ModuleInfo
import org.json.JSONObject

object ModuleSerializer {

    fun getModuleInfoFromJson(json: JSONObject): ModuleInfo? {
        try {
            return if (json.has("website")) {
                ModuleInfo(
                    json.getString("name"),
                    json.getString("author"),
                    json.getString("description"),
                    json.getString("main"),
                    json.getString("version"),
                    json.getString("website")
                )
            } else {
                ModuleInfo(
                    json.getString("name"),
                    json.getString("author"),
                    json.getString("description"),
                    json.getString("main"),
                    json.getString("version")
                )
            }
        } catch (e: Exception) {
            return null
        }
    }

}