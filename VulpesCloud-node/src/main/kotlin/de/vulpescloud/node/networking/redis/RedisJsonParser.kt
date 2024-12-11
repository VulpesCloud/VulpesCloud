package de.vulpescloud.node.networking.redis

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

}