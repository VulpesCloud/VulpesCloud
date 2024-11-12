package de.vulpescloud.bridge.service

import de.vulpescloud.api.network.redis.RedisHashNames
import de.vulpescloud.api.services.ClusterService
import de.vulpescloud.api.services.ClusterServiceFilter
import de.vulpescloud.api.services.ClusterServiceStates
import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.bridge.service.impl.ServiceImpl
import de.vulpescloud.bridge.task.impl.TaskImpl
import de.vulpescloud.wrapper.Wrapper
import org.json.JSONObject
import java.util.*
import org.jetbrains.annotations.ApiStatus

/**
 *   The ServiceProvider is used to manage the Services
 */
open class ServiceProvider {

    private var services: List<ClusterService> = listOf()

    /**
     *  This fun is used internally by Vulpescloud to get all Services from the Redis Hash.
     *  This fun should NOT be used by ANYTHING except the Wrapper!!!
     */
    @ApiStatus.Internal
    fun getAllServiceFromRedis() {
        val futureServices: MutableList<ClusterService> = mutableListOf()
        Wrapper.instance.getRC()?.getAllHashValues(RedisHashNames.VULPESCLOUD_SERVICES.name)?.forEach {
            val service = JSONObject(it)

            val taskJson = service.getJSONObject("task")
            futureServices.add(
                ServiceImpl(
                    getTaskFromJson(taskJson),
                    service.getInt("orderedId"),
                    UUID.fromString(service.getString("id")),
                    service.getInt("port"),
                    service.getString("hostname"),
                    service.getString("runningNode"),
                    service.getInt("maxPlayers"),
                    ClusterServiceStates.valueOf(service.getString("state"))
                )
            )
        }
        services = futureServices.toList()
    }

    /**
     * This fun is used to retrieve a List of all Services that are stored in the Redis Hash
     */
    fun services(): List<ClusterService> {
        return services
    }

    /**
     * This fun is used to retrieve a specific service by its name
     * @property name The name of the Service to find
     */
    fun findByName(name: String): ClusterService? {
        return services.find { it.name() == name }
    }

    /**
       This fun is used to retrieve a specific service by its id/UUID
    */
    fun findById(id: UUID): ClusterService? {
        return services.find { it.id() == id }
    }

    /**
       This fun is used to retrieve services by a ServiceFilter
    */
    fun findByFilter(filter: ClusterServiceFilter): List<ClusterService>? {
        return when (filter) {
            ClusterServiceFilter.ONLINE_SERVICES -> services.filter { it.state() == ClusterServiceStates.ONLINE }
            ClusterServiceFilter.EMPTY_SERVICES -> services.filter { it.isEmpty() }
            ClusterServiceFilter.PLAYERS_PRESENT_SERVERS -> services.filter { !it.isEmpty() }
            ClusterServiceFilter.SAME_NODE_SERVICES -> null // services.filter { Node.nodeConfig?.name == it.runningNode() } todo Get the Node of the Service and then check this
            ClusterServiceFilter.PROXIES -> services.filter { it.task().version().type == VersionType.PROXY }
            ClusterServiceFilter.SERVERS -> services.filter { it.task().version().type == VersionType.SERVER }
            ClusterServiceFilter.FALLBACKS -> services.filter { it.task().fallback() }
            ClusterServiceFilter.LOWEST_FALLBACK -> services.stream().filter { it.task().fallback() }.min(Comparator.comparingInt(
                ClusterService::onlinePlayersCount)).stream().toList()
        }?.toList()
    }

    /**
       This fun returns the Service that it is being called from
    */
    fun getLocalService(): ClusterService {
        val localService = findById(Wrapper.instance.service.id)
        if (localService == null) {
            throw NullPointerException()
        } else {
            return localService
        }
    }

    private fun getTaskFromJson(json: JSONObject): TaskImpl {
        val versionJson = json.getJSONObject("version")

        val version = VersionInfo(
            versionJson.getString("name"),
            VersionType.valueOf(versionJson.get("type").toString()),
            versionJson.getString("versions")
        )

        val nodesJson = json.getJSONArray("nodes")

        val nodes: Array<String?> = Array(nodesJson.length()) {
            if (nodesJson.isNull(it)) null else nodesJson.getString(it)
        }


        val templatesJson = json.getJSONArray("nodes")

        val templates: Array<String?> = Array(templatesJson.length()) {
            if (templatesJson.isNull(it)) null else templatesJson.getString(it)
        }

        val task = TaskImpl(
            json.getString("name"),
            json.getInt("maxMemory"),
            version,
            templates.toList(),
            nodes.toList(),
            json.getInt("maxPlayers"),
            json.getBoolean("staticServices"),
            json.getInt("minOnlineCount"),
            json.getBoolean("maintenance"),
            json.getInt("startPort"),
            json.getBoolean("fallback")
        )

        return task
    }

}