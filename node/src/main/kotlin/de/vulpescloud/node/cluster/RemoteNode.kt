package de.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.node.v1.ClusterAPIServiceGrpcKt
import build.buf.gen.vulpescloud.node.v1.getNodeSnapshotRequest
import build.buf.gen.vulpescloud.node.v1.snapshotOrNull
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeEndpointDetails
import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory

class RemoteNode(val endpoint: NodeEndpointDetails) {

    var channel: ManagedChannel? = null
    private val logger = LoggerFactory.getLogger("RemoteNode-${endpoint.name}")

    suspend fun getNode(): ClusterNode {
        return ClusterHelper.getNode(endpoint.name) ?: throw Exception("Node not found!")
    }

    suspend fun connect() {
        logger.info("Connecting to ${endpoint.name} at ${endpoint.host}:${endpoint.port}...")
        channel =
            ManagedChannelBuilder.forAddress(endpoint.host, endpoint.port).usePlaintext().build()

        val snapshot =
            try {
                ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineStub(channel!!)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))
                    .getNodeSnapshot(getNodeSnapshotRequest { this.name = endpoint.name })
                    .snapshotOrNull
            } catch (e: Exception) {
                logger.error(
                    "Failed to connect to ${endpoint.name} at ${endpoint.host}:${endpoint.port}!",
                    e.message,
                )
                null
            }

        val state = channel!!.getState(false)
        logger.info("Connection state from ${endpoint.name}: $state")
    }

    suspend fun reConnect() {
        channel?.shutdownNow()
        channel = null

        logger.info("Reconnecting to ${endpoint.name} at ${endpoint.host}:${endpoint.port}...")
        channel =
            ManagedChannelBuilder.forAddress(endpoint.host, endpoint.port).usePlaintext().build()

        val snapshot =
            try {
                ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineStub(channel!!)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))
                    .getNodeSnapshot(getNodeSnapshotRequest { this.name = endpoint.name })
                    .snapshotOrNull
            } catch (e: Exception) {
                logger.error(
                    "Failed to connect to ${endpoint.name} at ${endpoint.host}:${endpoint.port}!",
                    e.message,
                )
                null
            }

        val state = channel!!.getState(true)
        logger.info("Connection state from ${endpoint.name}: $state")
    }
}
