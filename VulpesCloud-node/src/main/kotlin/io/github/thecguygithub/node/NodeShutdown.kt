package io.github.thecguygithub.node

import io.github.thecguygithub.api.cluster.NodeStates
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.networking.redis.RedisConnectionChecker
import kotlin.system.exitProcess

object NodeShutdown {

    private val logger = Logger()

    fun nodeShutdown(forceShutdown: Boolean) {

        if (!forceShutdown) {

            Node.terminal!!.commandReadingThread.interrupt()

            logger.info("Shutting down the Node!")

            RedisConnectionChecker.instance.cancel()
            Node.instance!!.getRC()?.setHashField("VulpesCloud-HeartBeat", Node.nodeConfig!!.name, "0")
            Node.clusterProvider.updateLocalNodeState(NodeStates.OFFLINE)

            if (Node.redisController != null) {
                Node.redisController!!.shutdown()
            }

            Node.mySQLController.closedb()

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