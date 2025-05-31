package de.vulpescloud.node

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.slf4j.LoggerFactory

class HeartBeatScheduler(private val clusterProvider: ClusterProvider) : Scheduler() {

    private val heartbeatInterval = 5000L
    private val connectionLostCountUntilShutdown = 3
    private val heartbeatInvervalsUntilNodeLost = 5

    private val logger = LoggerFactory.getLogger("HeartBeatScheduler")

    private var localBeat = 0L
    private var localConnectionLostCount = 0
    private val oldHeartbeatMap = mutableMapOf<String, Long>()
    private val heartBeatFailureMap = mutableMapOf<String, Int>()

    override fun run() = launch {
        while (true) {
            getRC()
                ?.setHashField(
                    "VULPESCLOUD_NODE_HEARTBEAT",
                    clusterProvider.localNode().name,
                    localBeat.toString(),
                )
            localBeat++

            val localBeatFromRedis =
                getRC()
                    ?.getHashField("VULPESCLOUD_NODE_HEARTBEAT", clusterProvider.localNode().name)
                    ?.toLong() ?: 0L
            if (localBeatFromRedis == localBeat) {
                localConnectionLostCount = 0
            } else {
                localNodeLostConnection()
            }

            delay(heartbeatInterval)
        }
    }

    private fun localNodeLostConnection() {
        if (localConnectionLostCount >= connectionLostCountUntilShutdown) {
            logger.error(
                "Connection to Redis seems to be lost for too long, shutting down local node."
            )
            NodeShutdown.shutdownDueConnectionLost()
        } else {
            localConnectionLostCount++
            logger.warn("Local node lost connection to Redis, count: $localConnectionLostCount")
        }
    }

    private fun checkHeartbeatsAsHeadNode() {
        // Checks heartbeats of normal nodes in cluster and handles failures
        val currentBeats = getRC()?.getHashValuesAsPair("VULPESCLOUD_NODE_HEARTBEAT")
        currentBeats?.forEach { (name, beat) ->
            if (name == clusterProvider.localNode().name) return@forEach

            val oldBeat = oldHeartbeatMap[name] ?: 0L

            if (oldBeat < beat.toLong()) {
                logger.debug("Node $name is still alive!")
                heartBeatFailureMap[name] = 0
            } else {
                heartBeatFailureMap[name] = (heartBeatFailureMap[name] ?: 0) + 1
                if (heartBeatFailureMap[name]!! >= heartbeatInvervalsUntilNodeLost) {
                    logger.error(
                        "Node $name didn't send heartbeat for ${heartBeatFailureMap[name]} beats! Considering it lost."
                    )
                    val node = clusterProvider.nodeByName(name)!!
                    node.state = NodeStates.LOST
                    getRC()?.setHashField("VULPESCLOUD_NODES", name, JSONObject(node).toString())
                } else if (heartBeatFailureMap[name]!! < heartbeatInvervalsUntilNodeLost) {
                    logger.warn(
                        "Node $name didn't send heartbeat for ${heartBeatFailureMap[name]} beats!"
                    )
                }
            }
        }
    }

    private fun checkHeartbeatsAsNormalNode() {
        // Checks heartbeats of head node and handles head node failure

    }
}
