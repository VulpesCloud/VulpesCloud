package io.github.thecguygithub.node.tasks

import io.github.thecguygithub.api.Reloadable
import io.github.thecguygithub.api.platforms.PlatformGroupDisplay
import io.github.thecguygithub.api.tasks.ClusterTask
import io.github.thecguygithub.api.tasks.ClusterTaskProvider
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.networking.redis.RedisJsonParser
import io.github.thecguygithub.node.networking.redis.RedisManager
import org.json.JSONObject
import java.util.*
import java.util.concurrent.CompletableFuture


class ClusterTaskProviderImpl : ClusterTaskProvider(), Reloadable {

    private val groups: MutableSet<ClusterTask> = ClusterTaskFactory.readGroups().toMutableSet()
    private val redisManger = Node.instance?.getRC()?.let { RedisManager(it.getJedisPool()) }
    val logger = Logger()

    init {
        logger.debug("Subscribing")
        redisManger?.subscribe(mutableListOf("testcloud-events-group-create")) { _, channel, msg ->
            logger.debug("Received a Message")
            if (channel == "testcloud-events-group-create") {
                val message = msg?.let { RedisJsonParser.parseJson(it) }
                    ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                val json = RedisJsonParser.parseJson(message!!)
                logger.debug("Json is here!")

                logger.warn(message)
                logger.warn(json.toString())

                ClusterTaskFactory.createLocalStorageGroup(json, this)
            }
        }


        // val channelTransmit = clusterProvider.localNode().transmit()

        // channelTransmit.listen(GroupCreatePacket::class.java) { _, packet ->
        //     ClusterGroupFactory.createLocalStorageGroup(packet, this)
        // }

        // channelTransmit.listen(GroupDeletePacket::class.java) { _, packet ->
        //     ClusterGroupFactory.deleteLocalStorageGroup(packet.name, this)
        // }
    }

    override fun groupsAsync(): CompletableFuture<MutableSet<ClusterTask>>? {
        return CompletableFuture.completedFuture(groups)
    }

    override fun existsAsync(group: String?): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(groups.any { it.name().equals(group, ignoreCase = true) })
    }

    override fun deleteAsync(group: String?): CompletableFuture<Optional<String?>?> {
        val groupFuture = CompletableFuture<Optional<String?>?>()
        return groupFuture
    }

    override fun createAsync(
        name: String?,
        nodes: Array<String?>?,
        platform: PlatformGroupDisplay?,
        maxMemory: Int,
        staticService: Boolean,
        minOnline: Int,
        maintenance: Boolean,
    ): CompletableFuture<Optional<String?>?> {
        val taskFuture = CompletableFuture<Optional<String?>?>()
        val jsonTask = JSONObject()
        jsonTask.put("name", name)
        jsonTask.put("nodes", nodes)
        jsonTask.put("platforms", platform)
        jsonTask.put("maxMemory", maxMemory)
        jsonTask.put("staticService", staticService)
        jsonTask.put("minOnline", minOnline)
        jsonTask.put("maintenance", maintenance)

        Node.instance?.getRC()?.sendMessage(jsonTask.toString(), "testcloud-events-group-create")

        return taskFuture
    }

    override fun findAsync(group: String): CompletableFuture<ClusterTask?> {
        return CompletableFuture.completedFuture(groups.firstOrNull { it.name().equals(group, ignoreCase = true) })
    }

    override fun reload() {
        // groups.clear()
        // if (Node.instance().clusterProvider().localHead()) {
        //      groups.addAll(ClusterGroupFactory.readGroups())
        // } else {
        //     Node.instance().clusterProvider().headNode().transmit().request("groups-all", GroupCollectionPacket::class.java) {
        //         groups.addAll(it.groups)
        //        Logger().info("Successfully reload all group data.")
        //     }
        //     return
        // }
        Logger().info("Successfully reload all group data.")
    }
}