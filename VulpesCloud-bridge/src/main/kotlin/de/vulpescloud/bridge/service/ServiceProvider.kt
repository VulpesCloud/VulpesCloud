package de.vulpescloud.bridge.service

import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.bridge.json.ServiceSerializer.serviceFromJson
import de.vulpescloud.wrapper.Wrapper
import org.json.JSONObject
import java.util.*

object ServiceProvider {

    fun services(): List<Service> {
        val serviceList = mutableListOf<Service>()
        val list = Wrapper.instance.getRC()?.getAllHashValues(RedisHashNames.VULPESCLOUD_SERVICES.name)
        if (!list.isNullOrEmpty()) {
            list.forEach {
                val service = serviceFromJson(JSONObject(it))
                serviceList.add(service)
            }
        }
        return serviceList
    }

    fun findServiceById(id: UUID): Service? {
        return services().find { it.id() == id }
    }

    fun findServiceByName(name: String): Service? {
        return services().find { it.name().lowercase(Locale.getDefault()) == name.lowercase(Locale.getDefault()) }
    }

}