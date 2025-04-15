package de.vulpescloud.node

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.jediswrapper.JedisWrapper
import de.vulpescloud.node.cluster.ClusterProviderImpl
import de.vulpescloud.node.terminal.JLineTerminal
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.exitProcess

object NodeShutdown : KoinComponent {

    private val terminal: JLineTerminal by inject()
    private val clusterProvider: ClusterProvider by inject()

    fun ctrlCCloud() {
        terminal.close()
        exitProcess(0)
    }

    fun commandShutdown() {
        val clusterProv = clusterProvider as ClusterProviderImpl
        clusterProv.shutdown()

        JedisWrapper.getRC()?.shutdown()

        terminal.close()
        exitProcess(0)
    }

}