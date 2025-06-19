package de.vulpescloud.node

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.cluster.ClusterProviderImpl
import de.vulpescloud.node.module.ModuleProvider
import de.vulpescloud.node.mysql.DatabaseProvider
import de.vulpescloud.node.service.ServiceProviderImpl
import de.vulpescloud.node.service.ServiceScheduler
import de.vulpescloud.node.terminal.JLineTerminal
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object NodeShutdown : KoinComponent {

    private val logger = LoggerFactory.getLogger(NodeShutdown::class.java)
    private val terminal: JLineTerminal by inject()
    private val clusterProvider: ClusterProvider by inject()
    private val moduleProvider: ModuleProvider by inject()
    private val databaseProvider: DatabaseProvider by inject()
    private val serviceProvider: ServiceProvider by inject()
    private val serviceProviderImpl = serviceProvider as ServiceProviderImpl

    fun ctrlCCloud() {
        terminal.close()
        exitProcess(0)
    }

    fun shutdownDueConnectionLost() {
        logger.warn("Force stopping all LocalServices.")
        ServiceScheduler.cancel()
        serviceProviderImpl.localServices.forEach { it.forceStop() }
        terminal.close()
        exitProcess(0)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun commandShutdown() {
        GlobalScope.launch {
            logger.debug("Stopping ServiceScheduler")
            ServiceScheduler.cancel()

            logger.debug("Shutting down ClusterProvider")
            val clusterProv = clusterProvider as ClusterProviderImpl
            clusterProv.shutdown()

            logger.debug("Stopping LocalServices")
            serviceProviderImpl.localServices.forEach { it.sendCommand("stop") }
            serviceProviderImpl.loggingServices.clear()

            logger.info("Waiting for LocalServices to stop")
            while (serviceProviderImpl.localServices.isNotEmpty()) {
                logger.debug("Waiting for LocalServices to stop")
                serviceProviderImpl.localServices.forEach { it.sendCommand("stop") }
                delay(1000)
            }

            logger.debug("Deleting Heartbeat")
            getRC()?.deleteHashField("VULPESCLOUD:NODE:HEARTBEAT", clusterProvider.localNode().name)

            logger.debug("Unloading Modules")
            moduleProvider.unloadAllModules()

            logger.debug("Shutting down DatabaseProvider")
            databaseProvider.close()

            logger.debug("Shutting down RedisController")
            getRC()?.shutdown()

            logger.info("Goodbye!")
            terminal.close()
            exitProcess(0)
        }
    }
}
