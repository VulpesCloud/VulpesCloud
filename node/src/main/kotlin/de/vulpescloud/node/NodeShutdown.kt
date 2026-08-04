package de.vulpescloud.node

import de.vulpescloud.node.cluster.NodeSnapshotUpdater
import de.vulpescloud.node.event.EventListenHelper
import de.vulpescloud.node.services.ServiceLogHandler
import de.vulpescloud.node.services.ServiceScheduler
import java.util.concurrent.TimeoutException
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

@OptIn(ExperimentalAtomicApi::class)
object NodeShutdown {

    private val logger = LoggerFactory.getLogger("NodeShutdown")
    private val isShuttingDown = AtomicBoolean(false)

    suspend fun shutdown() {
        if (isShuttingDown.compareAndSet(expectedValue = false, newValue = true)) {
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
            Node.instance.internalEventsService.shutdown()

            logger.info("Closing connections to all remote nodes...")
            Node.instance.clusterProvider.remoteNodes.forEach { it.channel?.shutdownNow() }
            Node.instance.clusterProvider.shutdown()
            logger.info("DBG: State: ${Node.instance.clusterProvider.currentState}")
            NodeSnapshotUpdater.updateLocalNodeSnapshot()

            Node.instance.moduleProvider.apply {
                disableAllModules()
                unloadAllModules()
            }

            logger.info("Shutting down gRPC server...")
            Node.instance.grpcServer.stop()

            logger.info("Goodbye!")
            Node.instance.terminal.close()

            NodeCoroutineScope.cancel()

            exitProcess(0)
        } else {
            logger.warn("Node already shutting down...")
            logger.info("Force stopping node!")
            exitProcess(0)
        }
    }
}
