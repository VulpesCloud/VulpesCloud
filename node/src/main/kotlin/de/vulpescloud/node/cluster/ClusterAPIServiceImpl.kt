package de.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.node.v1.*
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeSnapshot
import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.annotations.RequiresPermission
import kotlinx.serialization.json.Json

class ClusterAPIServiceImpl : ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineImplBase() {
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
                nodesDatabase.get(request.name) ?: return GetNodeByNameResponse.newBuilder().build(),
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
}
