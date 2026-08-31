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

package org.vulpesstudios.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.auth.v1.getUserByExtraDataRequest
import build.buf.gen.vulpescloud.cluster.v2.*
import build.buf.gen.vulpescloud.cluster.v2.ClusterAPIServiceGrpcKt
import org.vulpesstudios.vulpescloud.api.cluster.NodeSnapshot
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.command.CommandSource
import org.vulpesstudios.vulpescloud.node.grpc.security.annotations.RequiresPermission
import org.vulpesstudios.vulpescloud.node.grpc.security.model.UserModel
import org.vulpesstudios.vulpescloud.node.utils.MongoUtils
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.incendo.cloud.exception.InvalidSyntaxException
import org.incendo.cloud.suggestion.Suggestion
import org.slf4j.LoggerFactory

class ClusterAPIServiceImpl : ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineImplBase() {
    private val logger = LoggerFactory.getLogger(ClusterAPIServiceImpl::class.java)
    private val snapshotsDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("nodeSnapshots")
    }
    private val json = Json

    @RequiresPermission("cluster.getAll")
    override suspend fun getAllNodes(request: GetAllNodesRequest): GetAllNodesResponse {
        val clusterConfig = Node.instance.clusterProvider.getClusterConfig()

        return GetAllNodesResponse.newBuilder()
            .addAllNodes(clusterConfig.nodes.map { it.toDefinition() })
            .build()
    }

    @RequiresPermission("cluster.get")
    override suspend fun getNodeByName(request: GetNodeByNameRequest): GetNodeByNameResponse {
        val clusterConfig = Node.instance.clusterProvider.getClusterConfig()
        val node =
            clusterConfig.nodes.firstOrNull {
                it.name.lowercase().contains(request.name.lowercase())
            } ?: return GetNodeByNameResponse.getDefaultInstance()

        return GetNodeByNameResponse.newBuilder().setNode(node.toDefinition()).build()
    }

    @RequiresPermission("cluster.getSnapshot")
    override suspend fun getNodeSnapshot(request: GetNodeSnapshotRequest): GetNodeSnapshotResponse {
        val snapshot =
            json.decodeFromJsonElement(
                NodeSnapshot.serializer(),
                snapshotsDatabase.get(request.name)
                    ?: return GetNodeSnapshotResponse.newBuilder().build(),
            )
        return GetNodeSnapshotResponse.newBuilder().setSnapshot(snapshot.toDefinition()).build()
    }

    @RequiresPermission("cluster.executeCommand")
    override suspend fun executeCommand(request: ExecuteCommandRequest): ExecuteCommandResponse {
        return withContext(Dispatchers.IO) {
            // TODO: Add hook for Metrics Module to track commands
            // TODO: Add support for UserCommandSource
            val source = CommandSource.player(getPlayer(request.playerSource.playerUuid)!!)

            runCatching {
                Node.instance.commandProvider
                    .execute(source, request.command)
                    .exceptionally { throw it }
                    .get()
            }
                .onFailure { e ->
                    when (e) {
                        is CompletionException,
                        is InvalidSyntaxException -> source.sendMessage(e.message.orEmpty())
                        is ExecutionException -> source.sendMessage(e.cause?.message.orEmpty())
                        else -> {
                            logger.error(
                                "An error occurred while executing command from ${request.playerSource.playerName}",
                                e,
                            )
                            source.sendMessage(
                                "An error occurred while executing command. Check Node-Logs for details."
                            )
                        }
                    }
                }

            ExecuteCommandResponse.newBuilder().addAllOutput(source.messages).build()
        }
    }

    @RequiresPermission("cluster.tabComplete")
    override suspend fun commandTabComplete(
        request: CommandTabCompleteRequest
    ): CommandTabCompleteResponse {
        val source = CommandSource.player(getPlayer(request.playerSource.playerUuid)!!)
        val suggestions =
            Node.instance.commandProvider.commandManager
                .suggestionFactory()
                .suggest(source, request.command)
                .join()
                .list()
                .stream()
                .map(Suggestion::suggestion)
                .toList()
        return CommandTabCompleteResponse.newBuilder().addAllSuggestions(suggestions).build()
    }

    private suspend fun getPlayer(uuid: String): UserModel? {
        return MongoUtils.getUserByName(
            Node.instance.localGrpcClient.authAPI
                .getUserByExtraData(
                    getUserByExtraDataRequest {
                        this.key = "minecraft-uuid"
                        this.value = uuid
                    }
                )
                .user
                .name
        )
    }
}
