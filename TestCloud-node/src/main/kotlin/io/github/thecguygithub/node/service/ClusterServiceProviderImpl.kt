package io.github.thecguygithub.node.service

import io.github.thecguygithub.api.Closeable
import io.github.thecguygithub.api.platforms.PlatformTypes
import io.github.thecguygithub.api.services.*
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.networking.RedisJsonParser
import io.github.thecguygithub.node.networking.RedisManager
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

class ClusterServiceProviderImpl : ClusterServiceProvider(), Closeable {

    @set:JvmName("setServiceProxyToken")
    var serviceProxyToken: String? = null
    private val services: MutableList<ClusterService> = CopyOnWriteArrayList()
    private val factory: ClusterServiceFactory = ClusterServiceFactoryImpl()
    private val clusterServiceQueue = ClusterServiceQueue()
    private val logReadingThread = ClusterLocalServiceReadingThread()

    private val logger = Logger()

    private val channels = mutableListOf("testcloud-service-actions", "testcloud-service-events")

    private val redisManger = Node.instance?.getRC()?.let { RedisManager(it.getJedisPool()) }

    init {

        redisManger?.subscribe(channels) {_, channel, msg ->
            if (channel == "testcloud-service-events") {
                val message = msg?.let { RedisJsonParser.parseJson(it) }
                    ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                val splitMsg = message?.split(";")

                if (splitMsg!![1] == "SERVICE") {
                    if (splitMsg[3] == "EVENT") {
                        if (splitMsg[4] == "START") {
                            if (splitMsg[5] != Node.nodeConfig?.localNode)
                            logger.info("The service &8'&f${splitMsg[2]}&8' &7is starting now&8...")
                            // services.add()
                        }
                    }
                }
            } else if (channel == "testcloud-service-actions") {
                val message = msg?.let { RedisJsonParser.parseJson(it) }
                    ?.let { RedisJsonParser.getMessagesFromRedisJson(it) }

                val splitMsg = message?.split(";")


            }
        }

        // localNode.transmit().responder("service-find") { property ->
        //     ClusterServicePacket(
        //         if (property.has("id")) find(property.getUUID("id"))
        //         else find(property.getString("name"))
        //     )
        // }

        // localNode.transmit().listen(ServiceShutdownCallPacket::class.java) { _, packet ->
        //     val service = Node.instance().serviceProvider().find(packet.id())
        //
        //     if (service == null) {
        //         log.error("Tried to shut down a service, but the service is not present in the cluster. {}", packet.id())
        //         return@listen
        //     }
        //
        //     service.shutdown()
        // }

//        localNode.transmit().listen(ServiceOnlinePacket::class.java) { transmit, packet ->
//            val service = find(packet.id())
//            if (service == null) {
//                transmit.channel().close()
//                return@listen
//            }
//
//            if (service is ClusterLocalServiceImpl) {
//                service.state = ClusterServiceState.ONLINE
//                service.update()
//            } else {
//                transmit.channel().close()
//                return@listen
//            }
//
//            log.info("The service &8'&f{}&8' &7is online&8.", service.name())
//            Node.instance().eventProvider().factory().call(ServiceOnlineEvent(service))
//        }

//        localNode.transmit().listen(ServiceCommandPacket::class.java) { transmit, packet ->
//            val service = find(packet.id())
//
//            if (isServiceChannel(transmit)) {
//                if (service is ClusterLocalServiceImpl) {
//                    service.executeCommand(packet.command())
//                } else {
//                    log.error("A node sent a command data for {}, but not a local instance.", packet.id())
//                }
//                return@listen
//            }
//
//            Node.instance().clusterProvider().find(service!!.runningNode()).transmit().sendPacket(packet)
//        }

//        localNode.transmit().responder("service-log") { property ->
//            val id = property.getUUID("id")
//            val service = Node.instance().serviceProvider().find(id)
//
//            if (service is ClusterLocalServiceImpl) {
//                return@responder ServiceLogPacket(service.logs())
//            }
//
//            val logs = Node.instance().clusterProvider().find(service!!.runningNode()).transmit()
//                .request("service-log", ServiceLogPacket::class.java, property).logs()
//            ServiceLogPacket(logs)
//        }

//        localNode.transmit().responder("service-players-count") { property ->
//            val service = find(property.getUUID("id"))
//
//            if (service is ClusterLocalServiceImpl) {
//                return@responder IntPacket(service.onlinePlayersCount())
//            }
//
//            IntPacket(
//                Node.instance().clusterProvider().find(service!!.runningNode()).transmit()
//                    .request("service-players-count", IntPacket::class.java, property).value
//            )
//        }

//        localNode.transmit().listen(RedirectPacket::class.java) { transmit, redirectPacket ->
//            val target = redirectPacket.target()
//            val service = find(target)
//
//            if (service is ClusterLocalServiceImpl) {
//                service.transmit().sendPacket(redirectPacket.packet())
//                return@listen
//            }
//
//            Node.instance().clusterProvider().find(service!!.runningNode()).transmit().sendPacket(redirectPacket)
//        }

//        localNode.transmit().responder("service-players") { property ->
//            PlayerCollectionPacket(find(property.getUUID("id"))!!.onlinePlayers())
//        }
//
//        localNode.transmit().responder("service-all") { _ ->
//            ServiceCollectionPacket(services)
//        }
//
//        localNode.transmit().responder("service-filtering") { property ->
//            ServiceCollectionPacket(find(property.getEnum("filter", ClusterServiceFilter::class.java)))
//        }
//
//        localNode.transmit().listen(ClusterSyncUnregisterServicePacket::class.java) { _, packet ->
//            Node.instance().serviceProvider().services()
//                .removeIf { it.id() == packet.id() }
//        }

        logReadingThread.start()
    }

    override fun servicesAsync(): CompletableFuture<MutableList<ClusterService>>? {
        return CompletableFuture.completedFuture(services)
    }

    override fun findAsync(id: UUID?): CompletableFuture<ClusterService?>? {
        return CompletableFuture.completedFuture(services.find { it.id() == id })
    }

    override fun findAsync(name: String?): CompletableFuture<ClusterService?>? {
        return CompletableFuture.completedFuture(services.find { it.name() == name })
    }

    override fun findAsync(filter: ClusterServiceFilter?): CompletableFuture<List<ClusterService?>?> {
        return CompletableFuture.completedFuture(
            when (filter) {
                ClusterServiceFilter.ONLINE_SERVICES -> services.filter { it.state(ClusterServiceStates.STARTING) == ClusterServiceStates.ONLINE }
                ClusterServiceFilter.EMPTY_SERVICES -> services.filter { it.isEmpty() }
                ClusterServiceFilter.PLAYERS_PRESENT_SERVERS -> services.filter { !it.isEmpty() }
                ClusterServiceFilter.SAME_NODE_SERVICES -> services.filter {
                    Node.nodeConfig?.localNode == it.runningNode()
                }
                ClusterServiceFilter.FALLBACKS -> services.filter { it.group().fallback() }
                ClusterServiceFilter.PROXIES -> services.filter { it.group().platform()?.type == PlatformTypes.PROXY }
                ClusterServiceFilter.SERVERS -> services.filter { it.group().platform()?.type == PlatformTypes.SERVER }
                ClusterServiceFilter.LOWEST_FALLBACK -> services.filter { it.group().fallback() }
                    .minByOrNull { it.onlinePlayersCount() }
                    ?.let { listOf(it) } ?: emptyList()

                null -> TODO()
            }
        )
    }

    override fun factory(): ClusterServiceFactory {
        return factory
    }

//    fun isServiceChannel(transmit: ChannelTransmit): Boolean {
//        return services.any { it is ClusterLocalServiceImpl && it.transmit() != null && it.transmit() == transmit }
//    }
//
//    fun find(transmit: ChannelTransmit): ClusterService? {
//        return services.find { it is ClusterLocalServiceImpl && it.transmit() != null && it.transmit() == transmit }
//    }

    override fun close() {
        logReadingThread.interrupt()
    }
}