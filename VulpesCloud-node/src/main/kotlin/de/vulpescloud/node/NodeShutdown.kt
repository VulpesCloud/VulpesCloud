package de.vulpescloud.node

import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.api.services.ServiceMessageBuilder
import de.vulpescloud.api.services.action.ServiceActions
import de.vulpescloud.node.networking.redis.RedisConnectionChecker
import de.vulpescloud.node.service.ServiceStartScheduler
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.system.exitProcess

object NodeShutdown {

    private val logger = LoggerFactory.getLogger(NodeShutdown::class.java)

    fun nodeShutdown(forceShutdown: Boolean) {

        if (!forceShutdown) {

            Node.terminal!!.commandReadingThread.interrupt()

            logger.info("Shutting down the Node!")

            Node.serviceProvider.services()?.forEach {
                Node.instance?.getRC()?.sendMessage(
                    ServiceMessageBuilder.actionMessageBuilder()
                        .setService(it)
                        .setAction(ServiceActions.STOP)
                        .build(),
                    RedisPubSubChannels.VULPESCLOUD_ACTION_SERVICE.name
                )
            }

            ServiceStartScheduler.instance.cancel()
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