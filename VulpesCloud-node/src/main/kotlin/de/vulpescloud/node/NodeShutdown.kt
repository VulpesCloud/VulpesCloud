package de.vulpescloud.node

import org.jetbrains.exposed.sql.transactions.transactionManager
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object NodeShutdown {
    private val logger = LoggerFactory.getLogger(NodeShutdown::class.java)

    fun normalShutdown() {
        Node.instance.getRC()?.shutdown()
        Node.instance.getDB()?.close()
        Node.instance.terminal.close()
        Node.instance.config.config.close()
        exitProcess(0)
    }

    fun forceShutdown(fully: Boolean) {
        if (fully) {
            Node.instance.terminal.close()
            Node.instance.config.config.close()
            exitProcess(1)
        } else {
            logger.error("The Node has been shut down forcefully, please press Ctrl + C to fully Exit the Cloud!")
            logger.error("This is implemented, so that you can look at the log, when the Cloud is used on Linux!")
            Node.instance.terminal.close()
            Node.instance.config.config.close()
        }
    }

}