package de.vulpescloud.node

import de.vulpescloud.node.cluster.NodeSnapshotUpdater
import de.vulpescloud.node.event.EventListenHelper
import de.vulpescloud.node.services.ServiceLogHandler
import de.vulpescloud.node.services.ServiceScheduler
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeoutException
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

object NodeShutdown {

    private val logger = LoggerFactory.getLogger("NodeShutdown")

    suspend fun shutdown() {
        logger.info("Shutting down the Node...")

        ServiceScheduler.stop()

        try {
            withTimeout(30.seconds) {
                coroutineScope {
                    Node.instance.nodeServices
                        .map { service ->
                            async {
                                logger.info(
                                    "Stopping ${service.service.task.name}-${service.service.orderedId}"
                                )
                                service.stop()
                            }
                        }
                        .awaitAll()
                    delay(1.seconds)
                }
            }
        } catch (_: TimeoutException) {
            logger.warn("Some services did not stop within 30 seconds, forcing shutdown...")
            Node.instance.nodeServices.forEach { it.delete() }
        }

        NodeSnapshotUpdater.stop()

        ServiceLogHandler.unsubscribe()
        EventListenHelper.unsubscribeFromEvents()

        logger.info("Closing connections to all remote nodes...")
        Node.instance.clusterProvider.remoteNodes.forEach { it.channel?.shutdownNow() }
        Node.instance.clusterProvider.shutdown()

        logger.info("Shutting down gRPC server...")
        Node.instance.grpcServer.stop()

        logger.info("Goodbye!")
        Node.instance.terminal.close()

        NodeCoroutineScope.cancel()

        exitProcess(0)
    }
}
