package dev.vulpescloud.bridge.service

import de.vulpescloud.api.network.redis.RedisHashNames
import de.vulpescloud.api.services.ClusterService
import de.vulpescloud.api.services.ClusterServiceFilter
import de.vulpescloud.api.services.ClusterServiceStates
import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.api.version.VersionType
import dev.vulpescloud.bridge.service.impl.ServiceImpl
import dev.vulpescloud.bridge.task.impl.TaskImpl
import io.github.thecguygithub.wrapper.Wrapper
import org.json.JSONObject
import java.util.*
import java.util.concurrent.CompletableFuture

open class ServiceProvider {

    private var services: List<ClusterService> = listOf()

    /*
    /   This fun is used internally by Vulpescloud to get all Services from the Redis Hash.
    /   This fun should NOT be used by ANYTHING except the Wrapper!!!
    */
    fun getAllServiceFromRedis() {
        val futureServices: MutableList<ClusterService> = mutableListOf()
        Wrapper.instance.getRC()?.getAllHashFields(RedisHashNames.VULPESCLOUD_SERVICES.name)?.forEach {
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

    fun services(): List<ClusterService> {
        return services
    }

    fun findByName(name: String): ClusterService? {
        return services.find { it.name() == name }
    }

    fun findById(id: UUID): ClusterService? {
        return services.find { it.id() == id }
    }

    fun findByFilter(filter: ClusterServiceFilter): List<ClusterService>? {
        return when (filter) {
            ClusterServiceFilter.ONLINE_SERVICES -> services.filter { it.state() == ClusterServiceStates.ONLINE }
            ClusterServiceFilter.EMPTY_SERVICES -> services.filter { it.isEmpty() }
            ClusterServiceFilter.PLAYERS_PRESENT_SERVERS -> services.filter { !it.isEmpty() }
            ClusterServiceFilter.SAME_NODE_SERVICES -> null // services.filter { Node.nodeConfig?.name == it.runningNode() } todo Get the Node of the Service and then check this
            ClusterServiceFilter.PROXIES -> services.filter { it.task().version().type == VersionType.PROXY }
            ClusterServiceFilter.SERVERS -> services.filter { it.task().version().type == VersionType.SERVER }
            ClusterServiceFilter.FALLBACKS -> null
            ClusterServiceFilter.LOWEST_FALLBACK -> services.stream().filter { it.task().fallback() }.min(Comparator.comparingInt(
                ClusterService::onlinePlayersCount)).stream().toList()
        }?.toList()
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