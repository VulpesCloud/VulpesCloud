package de.vulpescloud.node.services

import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.json.ServiceSerializer.serviceFromJson
import de.vulpescloud.node.json.TaskSerializer.taskFromJson
import org.json.JSONObject
import java.util.*

class ServiceProvider {
    private val localServices = mutableListOf<LocalServiceImpl>()

    fun services(): List<Service> {
        val serviceList = mutableListOf<Service>()
        val list = Node.instance.getRC()?.getAllHashValues(RedisHashNames.VULPESCLOUD_SERVICES.name)
        if (!list.isNullOrEmpty()) {
            list.forEach {
                val service = serviceFromJson(JSONObject(it))
                serviceList.add(service)
            }
        }
        return serviceList
    }

    fun localServices(): MutableList<LocalServiceImpl> {
        return localServices
    }

    fun findServiceById(id: UUID): Service? {
        return services().find { it.id() == id }
    }

    fun findServiceByName(name: String): Service? {
        return services().find { it.name().lowercase(Locale.getDefault()) == name.lowercase(Locale.getDefault()) }
    }

    fun findLocalServiceByName(name: String): LocalServiceImpl? {
        return localServices.find { it.name().lowercase(Locale.getDefault()) == name.lowercase(Locale.getDefault()) }
    }

    fun findLocalServiceById(id: UUID): LocalServiceImpl? {
        return localServices.find { it.id() == id }
    }

    //todo Implement service filtering
}