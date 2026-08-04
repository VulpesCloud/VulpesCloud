package de.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.node.v1.ClusterAPIServiceGrpcKt
import build.buf.gen.vulpescloud.node.v1.getNodeSnapshotRequest
import de.vulpescloud.api.cluster.NodeEndpointDetails
import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory

class RemoteNode(val endpoint: NodeEndpointDetails) {

    var channel: ManagedChannel? = null
    private val logger = LoggerFactory.getLogger("RemoteNode-${endpoint.name}")

    suspend fun reconnect() {
        channel?.shutdownNow()
        channel = null

        logger.info("Reconnecting to ${endpoint.name} at ${endpoint.host}:${endpoint.port}...")
        channel =
            ManagedChannelBuilder.forAddress(endpoint.host, endpoint.port).usePlaintext().build()

        try {
            ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineStub(channel!!)
                .withInterceptors(AuthClientInterceptor(Node.instance.secret))
                .getNodeSnapshot(getNodeSnapshotRequest { this.name = endpoint.name })
        } catch (e: Exception) {
            logger.error(
                "Failed to connect to ${endpoint.name} at ${endpoint.host}:${endpoint.port}!",
                e.message,
            )
        }

        val state = channel!!.getState(true)
        logger.info("Connection state from ${endpoint.name}: $state")
    }
}
