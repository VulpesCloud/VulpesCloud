package de.vulpescloud.node.utils

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeStates
import org.json.JSONObject
import java.util.*

object JsonUtils {

    fun getClusterNode(json: JSONObject): ClusterNode {
        return ClusterNode(
            json.getString("name"),
            UUID.fromString(json.getString("uuid")),
            json.getInt("runningServices"),
            NodeStates.valueOf(json.getString("state")),
            json.getInt("currentMemoryUsage"),
            json.getInt("maxMemoryUsage"),
            json.getString("cloudVersion"),
            json.getBoolean("isHeadNode")
        )
    }

}
