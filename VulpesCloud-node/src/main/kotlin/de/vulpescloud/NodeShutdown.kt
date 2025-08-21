package de.vulpescloud

import de.vulpescloud.node.Node
import org.slf4j.LoggerFactory

object NodeShutdown {

    private val logger = LoggerFactory.getLogger("NodeShutdown")

    fun shutdown() {
        logger.info("Shutting down the Node...")
        Node.instance.terminal.close()
    }

}
