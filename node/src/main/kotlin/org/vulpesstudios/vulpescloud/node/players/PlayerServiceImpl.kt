/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.players

import build.buf.gen.vulpescloud.players.v1.*
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.cluster.NodeState
import org.vulpesstudios.vulpescloud.api.players.OfflinePlayer
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.grpc.security.AuthClientInterceptor
import org.vulpesstudios.vulpescloud.node.grpc.security.annotations.RequiresPermission
import java.util.concurrent.TimeUnit

class PlayerServiceImpl : PlayersServiceGrpcKt.PlayersServiceCoroutineImplBase() {

    private val offlinePlayerDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("offlinePlayers")
    }
    private val stubCache =
        Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build<String, PlayersServiceGrpcKt.PlayersServiceCoroutineStub>()
    private val logger = LoggerFactory.getLogger(PlayerServiceImpl::class.java)

    @RequiresPermission("players.getAllOnline")
    override suspend fun getAllOnlinePlayers(
        request: GetAllOnlinePlayersRequest
    ): GetAllOnlinePlayersResponse {
        val onlinePlayers = mutableListOf<build.buf.gen.vulpescloud.players.v1.OnlinePlayer>()
        onlinePlayers.addAll(
            getAllOnlinePlayerOfNode(
                getAllOnlinePlayerOfNodeRequest {
                    this.name = Node.instance.configProvider.config.nodeName
                }
            )
                .playersList
        )
        Node.instance.clusterProvider.remoteNodes
            .filter { it.getSnapshot().state == NodeState.ONLINE }
            .forEach {
                val stub =
                    stubCache.get(it.endpoint.name) { _ ->
                        PlayersServiceGrpcKt.PlayersServiceCoroutineStub(it.channel!!)
                            .withInterceptors(AuthClientInterceptor(Node.instance.secret))
                    }

                onlinePlayers.addAll(
                    stub
                        .getAllOnlinePlayerOfNode(
                            getAllOnlinePlayerOfNodeRequest { this.name = it.endpoint.name }
                        )
                        .playersList
                )
            }

        return GetAllOnlinePlayersResponse.newBuilder().addAllOnlinePlayers(onlinePlayers).build()
    }

    @RequiresPermission("players.getAllOnlineOfNode")
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
                if (it.getSnapshot().state != NodeState.ONLINE) {
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

    @RequiresPermission("players.getAllOffline")
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
