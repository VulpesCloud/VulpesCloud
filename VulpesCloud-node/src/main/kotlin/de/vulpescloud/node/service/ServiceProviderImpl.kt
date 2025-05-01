package de.vulpescloud.node.service

import de.vulpescloud.api.service.Service
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.utils.JsonUtils.getService
import org.json.JSONObject
import java.util.*

class ServiceProviderImpl : ServiceProvider {

    val localServices = mutableListOf<LocalService>()
    val loggingServices = mutableListOf<String>()

    override fun getServiceByName(name: String): Service? {
        return services().find { it.name == name }
    }

    override fun getServiceByUUID(uuid: UUID): Service? {
        return services().find { it.uuid == uuid }
    }

    override fun services(): List<Service> {
        val services = mutableListOf<Service>()
        getRC()?.getAllHashValues("VULPESCLOUD_SERVICES")?.forEach {
            services.add(getService(JSONObject(it)))
        }

        return services
    }

    fun toggleServiceLogging(service: Service): Boolean {
        if (loggingServices.contains(service.name)) {
            loggingServices.remove(service.name)
            return false
        } else {
            loggingServices.add(service.name)
            return true
        }
    }

}
