package de.vulpescloud.node.networking.redis

import de.vulpescloud.node.Node
import org.json.JSONObject

object RedisJsonParser {

    fun parseJson(jsonString: String): JSONObject {
        return JSONObject(jsonString)
    }

    fun getTimeFromRedisJson(jsonObject: JSONObject): Long {
        return jsonObject.getLong("date")
    }

    fun getMessagesFromRedisJson(jsonObject: JSONObject): String {
        return jsonObject.getString("messages")
    }

    fun getActionFromRedisJson(jsonObject: JSONObject): String {
        return jsonObject.getString("action")
    }

    fun convert(string: String): JSONObject {
        val json =  JSONObject(string)
        if (json.getString("secret") == Node.instance.authenticationManager.getAuthToken()) {
            return JSONObject(json.getString("messages"))
        } else {
            throw IllegalAccessError("trying to parse message while secret is invalid")
        }
    }

}