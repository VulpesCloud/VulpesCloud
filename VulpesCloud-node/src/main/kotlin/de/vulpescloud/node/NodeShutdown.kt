package de.vulpescloud.node

import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.node.logging.Logger
import de.vulpescloud.node.networking.redis.RedisConnectionChecker
import kotlin.system.exitProcess

object NodeShutdown {

    private val logger = Logger()

    fun nodeShutdown(forceShutdown: Boolean) {

        if (!forceShutdown) {

            Node.terminal!!.commandReadingThread.interrupt()

            logger.info("Shutting down the Node!")

            Node.serviceProvider.services()?.forEach { Node.instance?.getRC()?.sendMessage("SERVICE;${it.id()};ACTION;STOP", "vulpescloud-action-service") }

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