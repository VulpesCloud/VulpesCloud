package io.github.thecguygithub.node

import io.github.thecguygithub.node.logging.Logger
import kotlin.system.exitProcess

object NodeShutdown {

    private val logger = Logger()

    fun nodeShutdown(forceShutdown: Boolean) {

        if (!forceShutdown) {

            Node.terminal!!.commandReadingThread.interrupt()

            logger.info("Shutting down the Node!")

            if (Node.redisController != null) {
                Node.redisController!!.shutdown()
            }

            Node.mySQLController!!.closedb()

            Node.terminal!!.close()

            exitProcess(0)
        } else {
            logger.error("")
            logger.error("STOPPING THE NODE IMMEDIATELY!")
            logger.error("")
            Node.terminal!!.close()
            exitProcess(0)
        }

    }
}