package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.Scheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.slf4j.LoggerFactory

class ClusterHeartbeatScheduler(private val clusterProvider: ClusterProvider) : Scheduler() {

    companion object {
        lateinit var instance: ClusterHeartbeatScheduler
    }

    private val heartbeatsUntilNewHead = 5
    private val heartbeatInterval = 1000L // 1 second
    private var localBeat = 0L
    private val oldHeartbeatMap = mutableMapOf<String, Long>()
    private val heartBeatFailureMap = mutableMapOf<String, Int>()
    private val logger = LoggerFactory.getLogger(ClusterHeartbeatScheduler::class.java)

    init {
        instance = this
    }

    override fun run() = launch {
        while (true) {
            getRC()
                ?.setHashField(
                    "VULPESCLOUD_NODE_HEARTBEAT",
                    clusterProvider.localNode().name,
                    localBeat.toString(),
                )
            localBeat++

            val currentBeats = getRC()?.getHashValuesAsPair("VULPESCLOUD_NODE_HEARTBEAT")

            currentBeats?.forEach { (name, beat) ->
                val oldBeat = oldHeartbeatMap[name]

                if (oldBeat != null) {
                    if (oldBeat < beat.toLong()) {
                        heartBeatFailureMap[name] = 0
                    } else {
                        heartBeatFailureMap[name] = (heartBeatFailureMap[name] ?: 0) + 1
                        if (clusterProvider.getHeadNode()?.name == name) {
                            logger.error("HeadNode didn't send heartbeat for ${heartBeatFailureMap[name]} beats!")
                        }
                    }
                } else {
                    heartBeatFailureMap[name] = 0
                }
            }

            oldHeartbeatMap.clear()
            currentBeats?.forEach { (name, beat) ->
                oldHeartbeatMap[name] = beat.toLong()
            }

            val failureHeadNode = clusterProvider.getHeadNode()?.name
            if (heartBeatFailureMap[failureHeadNode]!! >= heartbeatsUntilNewHead) {
                logger.error("HeadNode didn't update Heartbeat for more than $heartbeatsUntilNewHead beats!")
                val newHead = currentBeats?.maxByOrNull { it.value.toLong() }?.key

                if (clusterProvider.getHeadNode()?.name != newHead && clusterProvider.getHeadNode()?.name == failureHeadNode) {
                    logger.debug("Sending Message to promote a new HeadNode: $newHead")
                    getRC()?.sendMessage(
                        JSONObject()
                            .put("sender", clusterProvider.localNode().name)
                            .put("newHeadNodeName", newHead)
                            .toString(),
                        RedisChannels.VULPESCLOUD_CLUSTER_SelectNewHeadNode.name
                    )
                }
            }

            delay(heartbeatInterval)
        }
    }
}
