package de.vulpescloud.node.json

import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.node.json.TaskSerializer.taskFromJson
import de.vulpescloud.node.services.Service
import org.json.JSONObject
import java.util.*

object ServiceSerializer {

    fun serviceFromJson(json: JSONObject): Service {
        return Service(
            taskFromJson(json.getJSONObject("task")),
            json.getInt("orderedId"),
            UUID.fromString(json.getString("id")),
            json.getInt("port"),
            json.getString("hostname"),
            json.getString("runningNode"),
            json.getInt("maxPlayers"),
            ServiceStates.valueOf(json.getString("state")),
            json.getBoolean("logging")
        )
    }

    fun jsonFromService(service: Service): JSONObject {
        return JSONObject(service)
    }

}