package de.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.auth.v1.getUserByExtraDataRequest
import build.buf.gen.vulpescloud.node.v1.*
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeSnapshot
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.grpc.security.annotations.RequiresPermission
import de.vulpescloud.node.grpc.security.model.UserModel
import de.vulpescloud.node.utils.MongoUtils
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
    private val nodesDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("nodes")
    }
    private val snapshotsDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("nodeSnapshots")
    }
    private val json = Json

    @RequiresPermission("cluster.getAll")
    override suspend fun getAllNodes(request: GetAllNodesRequest): GetAllNodesResponse {
        val nodes =
            nodesDatabase
                .getAll()
                .map { json.decodeFromJsonElement(ClusterNode.serializer(), it) }
                .map { it.toDefinition() }
                .toMutableList()

        return GetAllNodesResponse.newBuilder().addAllNodes(nodes).build()
    }

    @RequiresPermission("cluster.get")
    override suspend fun getNodeByName(request: GetNodeByNameRequest): GetNodeByNameResponse {
        val node =
            json.decodeFromJsonElement(
                ClusterNode.serializer(),
                nodesDatabase.get(request.name)
                    ?: return GetNodeByNameResponse.newBuilder().build(),
            )

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
    override suspend fun tabComplete(request: TabCompleteRequest): TabCompleteResponse {
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
        return TabCompleteResponse.newBuilder().addAllSuggestions(suggestions).build()
    }

    override suspend fun ping(request: PingRequest): PingResponse {
        return PingResponse.newBuilder().build()
    }

    override suspend fun heartbeat(request: HeartbeatRequest): HeartbeatResponse {
        if (ClusterHelper.getHeadNode()!!.uuid != Node.instance.configProvider.config.uuid) {
            logger.warn("A node tried to send a heartbeat, but is not the head node!")
            throw RuntimeException("A node tried to send a heartbeat, but is not the head node!")
        }
        HeadNodeUtil.handleHeartbeat(request.node)
        return HeartbeatResponse.newBuilder().build()
    }

    override suspend fun authenticateNode(
        request: AuthenticateNodeRequest
    ): AuthenticateNodeResponse {
        val clusterNode =
            ClusterHelper.getNode(request.nodeName)
                ?: return AuthenticateNodeResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Not registered in Cluster Config!")
                    .build()

        if (clusterNode.uuid.toString() != request.nodeUuid)
            return AuthenticateNodeResponse.newBuilder()
                .setSuccess(false)
                .setMessage("UUID is not the same as in Cluster Config!")
                .build()
        val clusterConfig = Node.instance.clusterProvider.getClusterConfig()
        if (clusterConfig.nodes.none { it.uuid == clusterNode.uuid })
            return AuthenticateNodeResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Endpoint not registered in Cluster VConfig!")
                .build()

        return AuthenticateNodeResponse.newBuilder().setSuccess(true).build()
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
