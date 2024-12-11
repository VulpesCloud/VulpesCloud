package de.vulpescloud.node

import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.api.services.ServiceActions
import de.vulpescloud.api.services.builder.ServiceActionMessageBuilder
import de.vulpescloud.node.schedulers.ServiceStartScheduler
import de.vulpescloud.node.setups.FirstSetup
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.exitProcess

object NodeShutdown {
    private val lock = ReentrantLock()
    private val condition: Condition = lock.newCondition()
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
        CompletableFuture.runAsync() {
            lock.withLock {
                while (Node.instance.serviceProvider.services().isNotEmpty()) {
                    continue
                }
                condition.signalAll()
            }
        }
        lock.withLock {
            condition.await()
        }
        logger.info("Received Signal, continuing shutdown!")

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