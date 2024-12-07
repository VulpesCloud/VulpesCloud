package de.vulpescloud.bridge.service

import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.api.services.ServiceFilters
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.api.version.VersionType
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

    fun findServicesByFilter(filter: ServiceFilters): List<Service>? {
        return when (filter) {
            ServiceFilters.PREPARED_SERVICES -> services().filter { it.state() == ServiceStates.PREPARED }
            ServiceFilters.ONLINE_SERVICES -> services().filter { it.state() == ServiceStates.ONLINE}
            ServiceFilters.SERVERS -> services().filter { it.task().version().versionType == VersionType.SERVER.name }
            ServiceFilters.PROXIES -> services().filter { it.task().version().versionType == VersionType.PROXY.name }
            ServiceFilters.FALLBACKS -> services().filter { it.task().fallback() }
            ServiceFilters.EMPTY_SERVICES -> services().filter { it.isEmpty() }
            ServiceFilters.FULL_SERVICES -> services().filter { it.maxPlayers() <= it.onlinePlayersCount() }
            ServiceFilters.LOWEST_FALLBACK -> services().stream().filter { it.task().fallback() }.min(Comparator.comparingInt(
                Service::onlinePlayersCount)).stream().toList()
        }
    }

}