package de.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.node.v1.*
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeSnapshot
import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.annotations.RequiresPermission
import kotlinx.coroutines.flow.firstOrNull
import org.bson.BsonDocument
import org.bson.BsonString

class ClusterAPIServiceImpl : ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineImplBase() {

    @RequiresPermission("cluster.getAll")
    override suspend fun getAllNodes(request: GetAllNodesRequest): GetAllNodesResponse {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "nodes"
                )

        val nodes = mutableListOf<build.buf.gen.vulpescloud.node.v1.Node>()

        collection.find().collect {
            nodes.add(ClusterNode.fromDocument(it.toBsonDocument()).toDefinition())
        }

        return GetAllNodesResponse.newBuilder().addAllNodes(nodes).build()
    }

    @RequiresPermission("cluster.get")
    override suspend fun getNodeByName(request: GetNodeByNameRequest): GetNodeByNameResponse {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "nodes"
                )
        val filter =
            BsonDocument(
                "name",
                BsonDocument($$"$eq", BsonDocument("name", BsonString(request.name))),
            )
        val nodeDoc = collection.find(filter).firstOrNull()
        val node = nodeDoc?.let { ClusterNode.fromDocument(it) }
        if (node == null) {
            return GetNodeByNameResponse.newBuilder().build()
        }
        return GetNodeByNameResponse.newBuilder().setNode(node.toDefinition()).build()
    }

    @RequiresPermission("cluster.getSnapshot")
    override suspend fun getNodeSnapshot(request: GetNodeSnapshotRequest): GetNodeSnapshotResponse {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "nodeSnapshots"
                )
        val filter = BsonDocument("name", BsonString(request.name))
        val nodeSnapshotDoc = collection.find(filter).firstOrNull()
        val snapshot = nodeSnapshotDoc?.let { NodeSnapshot.fromDocument(it) }
        if (snapshot == null) {
            return GetNodeSnapshotResponse.newBuilder().build()
        }
        return GetNodeSnapshotResponse.newBuilder().setSnapshot(snapshot.toDefinition()).build()
    }
}
