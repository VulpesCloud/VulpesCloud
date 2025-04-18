package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.Scheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HeadNodeClusterHeartbeatScheduler(private val clusterProvider: ClusterProvider) :
    Scheduler() {

    companion object {
        lateinit var instance: HeadNodeClusterHeartbeatScheduler
    }

    init {
        instance = this
    }

    private val heartbeatInterval = 1000L // 1 second
    private var localBeat = 0L

    override fun run() = launch {
        while (true) {
            getRC()
                ?.setHashField(
                    "VULPESCLOUD_NODE_HEARTBEAT",
                    clusterProvider.localNode().name,
                    localBeat.toString(),
                )
            localBeat++

            delay(heartbeatInterval)
        }
    }
}
