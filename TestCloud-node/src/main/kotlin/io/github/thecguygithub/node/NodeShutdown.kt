package io.github.thecguygithub.node

import io.github.thecguygithub.node.logging.Logger
import lombok.extern.log4j.Log4j2
import kotlin.system.exitProcess


@Log4j2
object NodeShutdown {

    private val logger = Logger()

    fun nodeShutdown(forceShutdown: Boolean) {

        if (!forceShutdown) {

            logger.info("Shutting down the Node!")

            if (Node.redisController != null) {
                Node.redisController!!.shutdown()
            }

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