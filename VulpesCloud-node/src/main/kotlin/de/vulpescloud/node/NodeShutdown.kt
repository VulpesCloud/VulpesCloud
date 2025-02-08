package de.vulpescloud.node

import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.api.redis.builders.services.ServiceActionMessageBuilder
import de.vulpescloud.api.services.ServiceActions
import de.vulpescloud.node.schedulers.ServiceStartScheduler
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object NodeShutdown {
    private val logger = LoggerFactory.getLogger(NodeShutdown::class.java)

    @OptIn(DelicateCoroutinesApi::class)
    fun normalShutdown() {
        ServiceStartScheduler.cancel()

        logger.info("Stopping all Services!")
        Node.instance.serviceProvider.services()
            .forEach {
                Node.instance.getRC()?.sendMessage(
                    ServiceActionMessageBuilder
                        .setService(it)
                        .setAction(ServiceActions.STOP)
                        .build(),
                    RedisChannelNames.VULPESCLOUD_SERVICE_ACTION.name
                )
            }

        logger.info("Waiting for Signal to Shutdown!")
        val globalScope = GlobalScope.launch {

            Node.instance.serviceProvider.loggingServices.clear()

            var ttl = Node.instance.config.serviceStopTimeout
            while (Node.instance.serviceProvider.localServices().isNotEmpty()) {
                Node.instance.serviceProvider.localServices()
                    .forEach {
                        Node.instance.getRC()?.sendMessage(
                            ServiceActionMessageBuilder
                                .setService(it)
                                .setAction(ServiceActions.STOP)
                                .build(),
                            RedisChannelNames.VULPESCLOUD_SERVICE_ACTION.name
                        )
                    }

                if (ttl > 0) {
                    ttl -= 1
                    delay(1000)
                    continue
                } else {
                    Node.instance.serviceProvider.localServices()
                        .forEach {
                            it.destroyService()
                        }
                }
            }
        }

        runBlocking {
            globalScope.join()
        }

        logger.info("Received Signal, continuing shutdown!")
        logger.info("Stopping Modules!")
        Node.instance.moduleProvider.unloadAllModules()

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