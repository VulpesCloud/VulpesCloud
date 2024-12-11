package de.vulpescloud.bridge.json

import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.bridge.json.TaskSerializer.taskFromJson
import de.vulpescloud.bridge.service.Service
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
            json.getJSONObject("task").getInt("maxPlayers"),
            ServiceStates.valueOf(json.getString("state")),
            json.getBoolean("logging")
        )
    }

    fun jsonFromService(service: Service): JSONObject {
        return JSONObject(service)
    }

}