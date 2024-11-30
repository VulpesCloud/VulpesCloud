/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.node.service

import de.vulpescloud.api.network.redis.RedisHashNames
import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.api.services.ClusterService
import de.vulpescloud.api.services.ClusterServiceFactory
import de.vulpescloud.api.services.ClusterServiceFilter
import de.vulpescloud.api.services.ClusterServiceProvider
import de.vulpescloud.api.services.ClusterServiceStates
import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.node.Node
import de.vulpescloud.node.networking.redis.RedisJsonParser
import de.vulpescloud.node.networking.redis.RedisManager
import de.vulpescloud.node.task.TaskImpl
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

class ServiceProvider : ClusterServiceProvider() {

    private val services: MutableList<ClusterService> = CopyOnWriteArrayList()
    private val factory = ServiceFactory()
    private val logger = LoggerFactory.getLogger(ServiceProvider::class.java)
    private val channels = mutableListOf(
        RedisPubSubChannels.VULPESCLOUD_SERVICE_EVENT.name,
        RedisPubSubChannels.VULPESCLOUD_SERVICE_AUTH.name)
    private val redisManger = Node.instance?.getRC()?.let { RedisManager(it.getJedisPool()) }

    init {
        redisManger?.subscribe(channels) { _, channel, msg ->
            if (channel == RedisPubSubChannels.VULPESCLOUD_SERVICE_EVENT.name) {
                val message = msg?.let { RedisJsonParser.parseJson(it) }
                    ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                val splitMsg = message?.split(";")

                if (splitMsg!![0] == "SERVICE") {
                    if (splitMsg[2] == "EVENT") {
                        if (splitMsg[3] == "STATE") {
                            getAllServiceFromRedis()
                            logger.info(
                                Node.languageProvider.translate("service.event.state.message")
                                .replace("%name%", splitMsg[1])
                                .replace("%state%", splitMsg[4])
                            )
                            if (splitMsg[4] == "STOPPING") {
                                val service = Node.serviceProvider.findByName(splitMsg[1])
                                if (service != null) {
                                    Node.serviceProvider.services.remove(service)
                                    if (service.runningNode() == Node.nodeConfig!!.name) {
                                        Node.instance!!.getRC()?.deleteHashField(RedisHashNames.VULPESCLOUD_SERVICES.name, service.name())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    override fun factory(): ClusterServiceFactory {
        return factory
    }

    override fun findAsync(id: UUID?): CompletableFuture<ClusterService?>? {
        return CompletableFuture.completedFuture(services.find { it.id() == id })
    }

    override fun findAsync(name: String?): CompletableFuture<ClusterService?>? {
        return CompletableFuture.completedFuture(services.find { it.name() == name })
    }

    override fun findAsync(filter: ClusterServiceFilter): CompletableFuture<List<ClusterService?>?> {
        return CompletableFuture.completedFuture(
            when (filter) {
                ClusterServiceFilter.ONLINE_SERVICES -> services.filter { it.state() == ClusterServiceStates.ONLINE }
                ClusterServiceFilter.EMPTY_SERVICES -> services.filter { it.isEmpty() }
                ClusterServiceFilter.PLAYERS_PRESENT_SERVERS -> services.filter { !it.isEmpty() }
                ClusterServiceFilter.SAME_NODE_SERVICES -> services.filter { Node.nodeConfig?.name == it.runningNode() }
                ClusterServiceFilter.PROXIES -> services.filter { it.task().version().type == VersionType.PROXY }
                ClusterServiceFilter.SERVERS -> services.filter { it.task().version().type == VersionType.SERVER }
                ClusterServiceFilter.FALLBACKS -> null
                ClusterServiceFilter.LOWEST_FALLBACK -> services.stream().filter { it.task().fallback() }.min(Comparator.comparingInt(
                    ClusterService::onlinePlayersCount)).stream().toList()
            }?.toList()
        )
    }

    override fun servicesAsync(): CompletableFuture<MutableList<ClusterService>>? {
        return CompletableFuture.completedFuture(services)
    }

    fun getAllServiceFromRedis() {
        val futureServices: MutableList<ClusterService> = mutableListOf()
        Node.instance!!.getRC()?.getAllHashValues(RedisHashNames.VULPESCLOUD_SERVICES.name)?.forEach {
            val service = JSONObject(it)

            val taskJson = service.getJSONObject("task")
            futureServices.add(
                Service(
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

        services.forEach { services.remove(it) }

        futureServices.forEach { services.add(it) }
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