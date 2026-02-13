package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.NodeSnapshot
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.lang.management.ManagementFactory
import kotlin.time.Duration.Companion.seconds

object NodeSnapshotUpdater {

    private var job: Job? = null
    private val osMXBean =
        ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
    private val nodesDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("nodeSnapshots")
    }

    fun start() {
        job =
            NodeCoroutineScope.launch {
                while (true) {
                    val node = ClusterHelper.getLocalNode()

                    val snapshot =
                        NodeSnapshot(
                            Node.instance.configProvider.config.nodeName,
                            Node.instance.configProvider.config.uuid,
                            node.state,
                            usedMemory(),
                            osMXBean.cpuLoad,
                            0,
                            System.currentTimeMillis(),
                        )

                    nodesDatabase.upsert(node.name, Json.encodeToJsonElement(snapshot))

                    delay(5.seconds)
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun usedMemory(): Int {
        return ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) /
                1024 /
                1024)
            .toInt()
    }
}
