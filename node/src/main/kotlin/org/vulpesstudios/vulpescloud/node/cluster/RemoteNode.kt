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

import build.buf.gen.vulpescloud.cluster.v2.ClusterAPIServiceGrpcKt
import build.buf.gen.vulpescloud.cluster.v2.getNodeSnapshotRequest
import io.grpc.ManagedChannel
import io.grpc.netty.NettyChannelBuilder
import io.netty.handler.ssl.SslContext
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.cluster.NodeEndpointDetails
import org.vulpesstudios.vulpescloud.api.cluster.NodeSnapshot
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.grpc.security.AuthClientInterceptor

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
