package de.vulpescloud.node.command

import org.slf4j.LoggerFactory

class ConsoleCommandSource : CommandSource {

    private val logger = LoggerFactory.getLogger("ConsoleCommandSource")

    override fun sendMessage(message: String) {
        logger.info(message)
    }

    override fun sendError(message: String) {
        logger.error(message)
    }

}
