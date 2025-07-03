package de.vulpescloud.node

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.slf4j.LoggerFactory

class HeartBeatScheduler(private val clusterProvider: ClusterProvider) : Scheduler() {

    private val heartbeatInterval = 5000L
    private val connectionLostCountUntilShutdown = 3
    private val heartbeatIntervalsUntilNodeLost = 5
    private val heartbeatsUntilNewHead = 5

    private val logger = LoggerFactory.getLogger("HeartBeatScheduler")

    private var localBeat = 0L
    private var localConnectionLostCount = 0
    private val oldHeartbeatMap = mutableMapOf<String, Long>()
    private val heartBeatFailureMap = mutableMapOf<String, Int>()

    override fun run() = launch {
        while (true) {
            localBeat++
            getRC()
                ?.setHashField(
                    "VULPESCLOUD:NODE:HEARTBEAT",
                    clusterProvider.localNode().name,
                    localBeat.toString(),
                )

            val localBeatFromRedis =
                getRC()
                    ?.getHashField("VULPESCLOUD:NODE:HEARTBEAT", clusterProvider.localNode().name)
                    ?.toLong() ?: 0L
            if (localBeatFromRedis == localBeat) {
                localConnectionLostCount = 0

                if (clusterProvider.getHeadNode()?.name == clusterProvider.localNode().name) {
                    checkHeartbeatsAsHeadNode()
                } else {
                    checkHeartbeatsAsNormalNode()
                }
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
        val currentBeats = getRC()?.getHashValuesAsPair("VULPESCLOUD:NODE:HEARTBEAT")
        currentBeats?.forEach { (name, beat) ->
            if (name == clusterProvider.localNode().name) return@forEach

            val oldBeat = oldHeartbeatMap[name] ?: 0L

            if (oldBeat < beat.toLong()) {
                logger.debug("Node $name is still alive!")
                heartBeatFailureMap[name] = 0
            } else {
                heartBeatFailureMap[name] = (heartBeatFailureMap[name] ?: 0) + 1
                if (heartBeatFailureMap[name]!! >= heartbeatIntervalsUntilNodeLost) {
                    logger.error(
                        "Node $name didn't send heartbeat for ${heartBeatFailureMap[name]} beats! Considering it lost."
                    )
                    val node = clusterProvider.nodeByName(name)!!
                    node.state = NodeStates.LOST
                    getRC()?.setHashField("VULPESCLOUD:NODES", name, JSONObject(node).toString())
                    getRC()?.deleteHashField("VULPESCLOUD:NODE:HEARTBEAT", name)
                } else if (heartBeatFailureMap[name]!! < heartbeatIntervalsUntilNodeLost) {
                    logger.warn(
                        "Node $name didn't send heartbeat for ${heartBeatFailureMap[name]} beats!"
                    )
                }
            }
        }

        oldHeartbeatMap.clear()
        currentBeats?.forEach { (name, beat) -> oldHeartbeatMap[name] = beat.toLong() }
    }

    private fun checkHeartbeatsAsNormalNode() {
        val headNodeName = clusterProvider.getHeadNode()?.name ?: return
        val currentHeadNodeBeat =
            getRC()?.getHashField("VULPESCLOUD:NODE:HEARTBEAT", headNodeName)?.toLongOrNull() ?: 0L
        val oldHeadBeat = oldHeartbeatMap[headNodeName] ?: 0L
        val currentBeats =
            getRC()?.getHashValuesAsPair("VULPESCLOUD:NODE:HEARTBEAT")?.toMutableMap()

        if (oldHeadBeat < currentHeadNodeBeat) {
            logger.debug("Head node $headNodeName is still alive!")
            heartBeatFailureMap[headNodeName] = 0
        } else {
            heartBeatFailureMap[headNodeName] = (heartBeatFailureMap[headNodeName] ?: 0) + 1

            if (heartBeatFailureMap[headNodeName]!! >= heartbeatsUntilNewHead) {
                logger.error(
                    "HeadNode $headNodeName failed to update heartbeat! Choosing a new HeadNode."
                )
                currentBeats?.set(headNodeName, 0L.toString())

                val newHead = currentBeats?.maxByOrNull { it.value.toLong() }?.key
                if (newHead != null && newHead != headNodeName) {
                    logger.debug("Promoting new head node: $newHead")
                    getRC()
                        ?.sendMessage(
                            JSONObject()
                                .put("sender", clusterProvider.localNode().name)
                                .put("newHeadNodeName", newHead)
                                .toString(),
                            RedisChannels.VULPESCLOUD_CLUSTER_SelectNewHeadNode.name,
                        )
                }
            } else {
                logger.warn(
                    "Head node $headNodeName didn't send heartbeat for ${heartBeatFailureMap[headNodeName]} beats!"
                )
            }
        }

        oldHeartbeatMap.clear()
        currentBeats?.forEach { (name, beat) -> oldHeartbeatMap[name] = beat.toLong() }
    }
}
