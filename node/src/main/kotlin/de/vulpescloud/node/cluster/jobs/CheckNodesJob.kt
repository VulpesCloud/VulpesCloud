package de.vulpescloud.node.cluster.jobs

import build.buf.gen.vulpescloud.events.v1.nodeStateChangeEvent
import de.vulpescloud.api.cluster.NodeState
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.cluster.ClusterHelper
import de.vulpescloud.node.cluster.HeadNodeUtil
import de.vulpescloud.node.event.EventsService
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

object CheckNodesJob {
    private var job: Job? = null
    private val logger = LoggerFactory.getLogger(CheckNodesJob::class.java)

    fun start() {
        job = NodeCoroutineScope.launch {
            val config = Node.instance.clusterProvider.getClusterConfig()
            while (true) {
                delay(60.seconds)
                if (ClusterHelper.getHeadNode()?.uuid != Node.instance.configProvider.config.uuid) {
                    logger.warn(
                        "CheckNodeJob ran on a node that is not the head node! Stopping Job..."
                    )
                    break
                }

                val timeUntilNodeUnknownAndServiceLock =
                    Duration.parseOrNull(config.timeUntilNodeUnknownAndServiceLock) // Stage 1
                val timeUntilServiceRescheduling =
                    Duration.parseOrNull(config.timeUntilServiceRescheduling) // Stage 2
                val timeUntilNodeStable =
                    Duration.parseOrNull(config.timeUntilNodeStable) // Stage Reset

                if (
                    timeUntilNodeUnknownAndServiceLock == null ||
                        timeUntilServiceRescheduling == null ||
                        timeUntilNodeStable == null
                ) {
                    logger.error(
                        "Invalid time values in cluster config! Check values and restart node!"
                    )
                    continue
                }

                val latestHeartbeats = HeadNodeUtil.nodeHeartbeats.toMap()
                val currentTime = System.currentTimeMillis().milliseconds
                latestHeartbeats.forEach { (uuid, heartbeat) ->
                    val node =
                        ClusterHelper.getAllNodes().find { it.uuid == uuid } ?: return@forEach

                    if (node.state == NodeState.UNKNOWN) {
                        if (currentTime - heartbeat < 15.seconds) {
                            val newNode =
                                node.copy(
                                    state = NodeState.ONLINE,
                                    attributes =
                                        node.attributes.toMutableMap().apply {
                                            set("connStable", "false")
                                            set("connUnstableTime", currentTime.toString())
                                        },
                                )
                            ClusterHelper.updateNode(newNode)
                            EventsService.publish(
                                nodeStateChangeEvent {
                                    this.node = newNode.toDefinition()
                                    this.oldState = node.state.toNodeStates()
                                    this.newState = NodeState.ONLINE.toNodeStates()
                                },
                                true,
                            )
                            logger.info(
                                "Node ${node.name} has returned from being AFK! Tagging with Unstable Connection!"
                            )
                        }

                        return@forEach
                    }

                    if (currentTime - heartbeat > timeUntilNodeUnknownAndServiceLock) {
                        val time = (currentTime - heartbeat)
                        logger.warn(
                            "Node ${node.name} has not sent heartbeat in $time! Marking node as unknown and locking services!"
                        )
                        val newNode = node.copy(state = NodeState.UNKNOWN)
                        ClusterHelper.updateNode(newNode)
                        EventsService.publish(
                            nodeStateChangeEvent {
                                this.node = newNode.toDefinition()
                                this.oldState = node.state.toNodeStates()
                                this.newState = NodeState.UNKNOWN.toNodeStates()
                            },
                            true,
                        )
                        // TODO: lock services
                        return@forEach
                    }

                    if (currentTime - heartbeat > 15.seconds) {
                        val time = (currentTime - heartbeat)
                        logger.warn("Node ${node.name} has not sent heartbeat in $time!")
                        val newNode =
                            node.copy(
                                attributes =
                                    node.attributes.toMutableMap().apply {
                                        set("connStable", "false")
                                    }
                            )
                        ClusterHelper.updateNode(newNode)
                    }

                    if (node.attributes.contains("connUnstableTime")) {
                        val unstableTime =
                            Duration.parseOrNull(node.attributes["connUnstableTime"]!!)
                        if (unstableTime == null) {
                            logger.error(
                                "connUnstableTime is not a valid duration! (${node.name}, ${node.uuid})"
                            )
                            return@forEach
                        }

                        if (currentTime - unstableTime > timeUntilNodeStable) {
                            logger.info("Node ${node.name} is now marked as stable!")
                            val newNode =
                                node.copy(
                                    attributes =
                                        node.attributes.toMutableMap().apply {
                                            set("connStable", "true")
                                            remove("connUnstableTime")
                                        }
                                )
                            ClusterHelper.updateNode(newNode)
                        }
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
