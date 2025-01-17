package de.vulpescloud.api.redis.builders

import de.vulpescloud.api.cluster.NodeActions
import org.json.JSONObject

object NodeAuthMessageBuilder {
    private var nodeName: String? = null
    private var secret: String? = null

    fun setNodeName(nodeName: String): NodeAuthMessageBuilder {
        this.nodeName = nodeName
        return this
    }

    fun setSecret(secret: String): NodeAuthMessageBuilder {
        this.secret = secret
        return this
    }

    fun build(): String {
        val json = JSONObject()

        json.put("action", NodeActions.NODE_AUTHENTICATION.name)
        json.put("secret", secret!!)
        json.put("nodeName", nodeName!!)

        return json.toString()
    }
}