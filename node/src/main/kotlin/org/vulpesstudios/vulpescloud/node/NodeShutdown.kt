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

package org.vulpesstudios.vulpescloud.node

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.node.cluster.NodeSnapshotUpdater
import org.vulpesstudios.vulpescloud.node.event.EventListenHelper
import org.vulpesstudios.vulpescloud.node.services.ServiceLogHandler
import java.util.concurrent.TimeoutException
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalAtomicApi::class)
object NodeShutdown {

    private val logger = LoggerFactory.getLogger("NodeShutdown")
    private val isShuttingDown = AtomicBoolean(false)

    suspend fun shutdown() {
        if (isShuttingDown.compareAndSet(expectedValue = false, newValue = true)) {
            logger.info("Shutting down the Node...")

            Node.instance.chronyxCoordinator.stop()

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
            Node.instance.clusterProvider.shutdown()
            Node.instance.clusterProvider.remoteNodes.forEach { it.channel?.shutdownNow() }
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
