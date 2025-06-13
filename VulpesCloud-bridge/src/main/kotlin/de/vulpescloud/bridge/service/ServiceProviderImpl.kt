package de.vulpescloud.bridge.service

import de.vulpescloud.api.service.ServiceInfo
import de.vulpescloud.api.service.ServiceFilter
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.bridge.JsonUtils.getService
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import java.util.*
import kotlin.Comparator
import kotlin.jvm.optionals.toList
import org.json.JSONObject

class ServiceProviderImpl : ServiceProvider {
    override fun getServiceByName(name: String): ServiceInfo? {
        return services().find { it.name == name }
    }

    override fun getServiceByUUID(uuid: UUID): ServiceInfo? {
        return services().find { it.uuid == uuid }
    }

    override fun services(): List<ServiceInfo> {
        val services = mutableListOf<ServiceInfo>()
        getRC()?.getAllHashValues("VULPESCLOUD_SERVICES")?.forEach {
            services.add(getService(JSONObject(it)))
        }

        return services
    }

    fun getLocalService(): ServiceInfo {
        return services().find { it.name == System.getenv("serviceName") }!!
    }

    override fun getServicesByFilter(filter: ServiceFilter): List<ServiceInfo> {
        return when (filter) {
            ServiceFilter.PREPARED_SERVICES -> {
                services().filter { it.state == ServiceStates.PREPARED }
            }
            ServiceFilter.EMPTY_SERVICES -> {
                services().filter { it.onlinePlayerCount == 0 }
            }
            ServiceFilter.FULL_SERVICES -> {
                services().filter { it.onlinePlayerCount == it.maxPlayers }
            }
            ServiceFilter.FALLBACKS -> {
                services().filter { it.task.fallback }
            }
            ServiceFilter.LOWEST_FALLBACK -> {
                services()
                    .filter { it.task.fallback }
                    .stream()
                    .min(Comparator.comparingInt(ServiceInfo::onlinePlayerCount))
                    .toList()
            }
            ServiceFilter.ONLINE_SERVICES -> {
                services().filter { it.state == ServiceStates.ONLINE }
            }
            ServiceFilter.PROXIES -> {
                services().filter { it.task.version.type == VersionType.PROXY }
            }
            ServiceFilter.SERVERS -> {
                services().filter { it.task.version.type == VersionType.SERVER }
            }
        }
    }
}
