package de.vulpescloud.node

import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.api.services.ServiceMessageBuilder
import de.vulpescloud.api.services.action.ServiceActions
import de.vulpescloud.node.networking.redis.RedisConnectionChecker
import de.vulpescloud.node.service.ServiceStartScheduler
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object NodeShutdown {

    private val logger = LoggerFactory.getLogger(NodeShutdown::class.java)

    @OptIn(DelicateCoroutinesApi::class)
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
                    RedisPubSubChannels.VULPESCLOUD_SERVICE_ACTION.name
                )
            }

            GlobalScope.launch {
                logger.info("Waiting 10 seconds for all Services to Stop!")

                delay(10000)

                ServiceStartScheduler.instance.cancel()
                RedisConnectionChecker.instance.cancel()
                Node.instance!!.getRC()?.setHashField("VulpesCloud-HeartBeat", Node.nodeConfig!!.name, "0")
                Node.clusterProvider.updateLocalNodeState(NodeStates.OFFLINE)

                if (Node.redisController != null) {
                    Node.redisController!!.shutdown()
                }

                Node.mySQLController.closedb()

                logger.info("Waiting 1 second to ensure that the database connection is closed!")

                delay(1000)

                Node.terminal!!.close()

                exitProcess(0)
            }
        } else {
            logger.error("")
            logger.error("STOPPING THE NODE IMMEDIATELY!")
            logger.error("")
            Node.terminal!!.close()
            exitProcess(0)
        }

    }
}