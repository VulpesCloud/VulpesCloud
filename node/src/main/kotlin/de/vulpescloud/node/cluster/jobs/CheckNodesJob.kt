package de.vulpescloud.node.cluster.jobs

import de.vulpescloud.api.cluster.ClusterConfig
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.cluster.ClusterHelper
import de.vulpescloud.node.cluster.HeadNodeUtil
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object CheckNodesJob {
    private var job: Job? = null
    private val logger = LoggerFactory.getLogger(CheckNodesJob::class.java)

    fun start() {
        job = NodeCoroutineScope.launch {
            val config = Node.instance.clusterProvider.getClusterConfig()
            while (true) {
                delay(60.seconds)
                if (ClusterHelper.getHeadNode()?.uuid != Node.instance.configProvider.config.uuid) {
                    logger.warn("CheckNodeJob ran on a node that is not the head node! Stopping Job...")
                    break
                }

                val timeUntilNodeUnknownAndServiceLock = Duration.parseOrNull(config.timeUntilNodeUnknownAndServiceLock) // Stage 1
                val timeUntilServiceRescheduling = Duration.parseOrNull(config.timeUntilServiceRescheduling) // Stage 2
                val timeUntilNodeStable = Duration.parseOrNull(config.timeUntilNodeStable) // Stage Reset

                if (timeUntilNodeUnknownAndServiceLock == null || timeUntilServiceRescheduling == null || timeUntilNodeStable == null) {
                    logger.error("Invalid time values in cluster config! Check values and restart node!")
                    continue
                }

                val latestHeartbeats = HeadNodeUtil.nodeHeartbeats.toMap()
                val currentTime = System.currentTimeMillis().milliseconds
                latestHeartbeats.forEach { (uuid, heartbeat) ->
                    val node = ClusterHelper.getAllNodes().find { it.uuid == uuid } ?: return@forEach
                    if (currentTime - heartbeat > timeUntilNodeUnknownAndServiceLock) {
                        val time = (timeUntilNodeUnknownAndServiceLock - (currentTime - heartbeat))
                        logger.warn("Node ${node.name} has not sent heartbeat in $time! #TODO")
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
