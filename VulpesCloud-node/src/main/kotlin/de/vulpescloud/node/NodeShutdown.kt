package de.vulpescloud.node

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.cluster.ClusterHeartbeatScheduler
import de.vulpescloud.node.cluster.ClusterProviderImpl
import de.vulpescloud.node.module.ModuleProvider
import de.vulpescloud.node.terminal.JLineTerminal
import kotlin.system.exitProcess
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object NodeShutdown : KoinComponent {

    private val terminal: JLineTerminal by inject()
    private val clusterProvider: ClusterProvider by inject()
    private val moduleProvider: ModuleProvider by inject()

    fun ctrlCCloud() {
        terminal.close()
        exitProcess(0)
    }

    fun commandShutdown() {

        ClusterHeartbeatScheduler.instance.cancel()

        getRC()?.deleteHashField("VULPESCLOUD_NODE_HEARTBEAT", clusterProvider.localNode().name)

        moduleProvider.unloadAllModules()

        val clusterProv = clusterProvider as ClusterProviderImpl
        clusterProv.shutdown()

        getRC()?.shutdown()

        terminal.close()
        exitProcess(0)
    }
}
