package de.vulpescloud.node

import de.vulpescloud.node.services.ServiceLogHandler
import kotlinx.coroutines.cancel
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object NodeShutdown {

    private val logger = LoggerFactory.getLogger("NodeShutdown")

    suspend fun shutdown() {
        logger.info("Shutting down the Node...")

        Node.instance.nodeServices.forEach {
            logger.info("Stopping ${it.service.task.name}-${it.service.orderedId}")
            it.stop()
        }

        ServiceLogHandler.subscribe()

        Node.instance.grpcServer.stop()

        Node.instance.terminal.close()

        NodeCoroutineScope.cancel()

        exitProcess(0)
    }
}
