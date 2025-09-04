package de.vulpescloud.node

import org.slf4j.LoggerFactory

object NodeShutdown {

    private val logger = LoggerFactory.getLogger("NodeShutdown")

    fun shutdown() {
        logger.info("Shutting down the Node...")

        Node.instance.nodeServices.forEach { it.stop() }

        Node.instance.terminal.close()
    }
}
