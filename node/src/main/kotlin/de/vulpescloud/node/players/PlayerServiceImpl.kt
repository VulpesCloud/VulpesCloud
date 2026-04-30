package de.vulpescloud.node.players

import build.buf.gen.vulpescloud.players.v1.GetAllOnlinePlayerOfNodeRequest
import build.buf.gen.vulpescloud.players.v1.GetAllOnlinePlayerOfNodeResponse
import build.buf.gen.vulpescloud.players.v1.GetAllOnlinePlayersRequest
import build.buf.gen.vulpescloud.players.v1.GetAllOnlinePlayersResponse
import build.buf.gen.vulpescloud.players.v1.GetOfflinePlayersRequest
import build.buf.gen.vulpescloud.players.v1.GetOfflinePlayersResponse
import build.buf.gen.vulpescloud.players.v1.PlayersServiceGrpcKt
import build.buf.gen.vulpescloud.players.v1.getOfflinePlayersResponse
import com.github.benmanes.caffeine.cache.Caffeine
import de.vulpescloud.api.players.OfflinePlayer
import de.vulpescloud.api.players.OnlinePlayer
import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class PlayerServiceImpl : PlayersServiceGrpcKt.PlayersServiceCoroutineImplBase() {

    private val offlinePlayerDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("offlinePlayers")
    }
    private val stubCache =
        Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build<String, PlayersServiceGrpcKt.PlayersServiceCoroutineStub>()
    private val logger = LoggerFactory.getLogger(PlayerServiceImpl::class.java)

    override suspend fun getAllOnlinePlayers(
        request: GetAllOnlinePlayersRequest
    ): GetAllOnlinePlayersResponse {
        val onlinePlayers = mutableListOf<build.buf.gen.vulpescloud.players.v1.OnlinePlayer>()
        Node.instance.clusterProvider.remoteNodes
            .filter { it.getNode().isRunning() }
            .forEach {
                val stub =
                    stubCache.get(it.endpoint.name) { _ ->
                        PlayersServiceGrpcKt.PlayersServiceCoroutineStub(it.channel!!)
                            .withInterceptors(AuthClientInterceptor(Node.instance.secret))
                    }

                onlinePlayers.addAll(stub.getAllOnlinePlayers(request).onlinePlayersList)
            }

        return GetAllOnlinePlayersResponse.newBuilder().addAllOnlinePlayers(onlinePlayers).build()
    }

    override suspend fun getAllOnlinePlayerOfNode(
        request: GetAllOnlinePlayerOfNodeRequest
    ): GetAllOnlinePlayerOfNodeResponse {
        val nodeName = request.name
        if (nodeName == Node.instance.configProvider.config.nodeName) {
            val onlinePlayers = mutableListOf<build.buf.gen.vulpescloud.players.v1.OnlinePlayer>()
            Node.instance.nodeProxyPlayers.values.forEach {
                it.forEach { player -> onlinePlayers.add(player.toProto()) }
            }

            return GetAllOnlinePlayerOfNodeResponse.newBuilder()
                .addAllPlayers(onlinePlayers)
                .build()
        }
        Node.instance.clusterProvider.remoteNodes
            .find { it.endpoint.name == nodeName }
            ?.let {
                if (!it.getNode().isRunning()) {
                    logger.error(
                        "Received request to get all online players of node $nodeName but node is not online!"
                    )
                    return GetAllOnlinePlayerOfNodeResponse.newBuilder().build()
                }
                val stub =
                    stubCache.get(it.endpoint.name) { _ ->
                        PlayersServiceGrpcKt.PlayersServiceCoroutineStub(it.channel!!)
                            .withInterceptors(AuthClientInterceptor(Node.instance.secret))
                    }
                return stub.getAllOnlinePlayerOfNode(request)
            }
        logger.error(
            "Received request to get all online players of node $nodeName wich could not be found!"
        )
        return GetAllOnlinePlayerOfNodeResponse.newBuilder().build()
    }

    override suspend fun getAllOfflinePlayers(
        request: GetOfflinePlayersRequest
    ): GetOfflinePlayersResponse {
        val players = offlinePlayerDatabase.getAll()
        return getOfflinePlayersResponse {
            this.offlinePlayers.addAll(
                players.map { Json.decodeFromJsonElement(OfflinePlayer.serializer(), it).toProto() }
            )
        }
    }
}
