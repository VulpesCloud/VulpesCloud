package de.vulpescloud.node.cluster.jobs

import build.buf.gen.vulpescloud.node.v1.ClusterAPIServiceGrpcKt
import build.buf.gen.vulpescloud.node.v1.heartbeatRequest
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.cluster.ClusterHelper
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory

object HeartbeatJob {
    private var job: Job? = null
    private val logger = LoggerFactory.getLogger(HeartbeatJob::class.java)

    fun start() {
        job = NodeCoroutineScope.launch {
            while (true) {
                val headNode = ClusterHelper.getHeadNode()
                val channel =
                    Node.instance.clusterProvider.remoteNodes
                        .find { it.endpoint.uuid == headNode!!.uuid }
                        ?.channel
                if (channel != null) {
                    runCatching {
                            withTimeout(5.seconds) {
                                val stub =
                                    ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineStub(channel)
                                        .withInterceptors(
                                            AuthClientInterceptor(Node.instance.secret)
                                        )
                                stub.heartbeat(
                                    heartbeatRequest {
                                        this.node = ClusterHelper.getLocalNode().toDefinition()
                                    }
                                )
                            }
                        }
                        .onFailure { e ->
                            logger.error("Unable to send heartbeat to head node! ${e.message}")
                        }
                }
                // TODO: Do something if HeadNode does not accept heartbeat (Sync with other nodes
                // if possible)

                delay(10.seconds)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
