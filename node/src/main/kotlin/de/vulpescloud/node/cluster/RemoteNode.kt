package de.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.cluster.v2.ClusterAPIServiceGrpcKt
import build.buf.gen.vulpescloud.cluster.v2.getNodeSnapshotRequest
import de.vulpescloud.api.cluster.NodeEndpointDetails
import de.vulpescloud.api.cluster.NodeSnapshot
import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import io.grpc.ManagedChannel
import io.grpc.netty.NettyChannelBuilder
import io.netty.handler.ssl.SslContext
import org.slf4j.LoggerFactory

class RemoteNode(val endpoint: NodeEndpointDetails) {

    var channel: ManagedChannel? = null
    private val logger = LoggerFactory.getLogger("RemoteNode-${endpoint.name}")

    suspend fun reconnect(sslContext: SslContext? = null) {
        channel?.shutdownNow()
        channel = null

        logger.info("Reconnecting to ${endpoint.name} at ${endpoint.host}:${endpoint.port}...")
        val channelBuilder = NettyChannelBuilder.forAddress(endpoint.host, endpoint.port)
        if (sslContext != null) {
            channelBuilder.sslContext(sslContext)
        } else {
            channelBuilder.usePlaintext()
        }
        channel = channelBuilder.build()

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

    suspend fun getSnapshot(): NodeSnapshot {
        return NodeSnapshot.fromDefinition(
            Node.instance.localGrpcClient.clusterAPI
                .getNodeSnapshot(getNodeSnapshotRequest { this.name = endpoint.name })
                .snapshot
        )
    }
}
