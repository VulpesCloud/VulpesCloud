package de.vulpescloud.bridge.service

import de.vulpescloud.api.service.Service
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.bridge.JsonUtils.getService
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import org.json.JSONObject
import java.util.*

class ServiceProviderImpl : ServiceProvider {
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

    fun getLocalService(): Service {
        return services().find { it.name == System.getenv("serviceName") }!!
    }
}
