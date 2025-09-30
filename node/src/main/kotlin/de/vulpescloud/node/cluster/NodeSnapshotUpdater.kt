package de.vulpescloud.node.cluster

import com.mongodb.client.model.ReplaceOptions
import de.vulpescloud.api.cluster.NodeSnapshot
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bson.BsonDocument
import org.bson.BsonString
import java.lang.management.ManagementFactory
import kotlin.time.Duration.Companion.seconds

object NodeSnapshotUpdater {

    private var job: Job? = null
    private val osMXBean =
        ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean

    fun start() {
        job =
            NodeCoroutineScope.launch {
                while (true) {
                    val collection =
                        Node.instance.mongoClient
                            .getDatabase(Node.instance.configProvider.config.mongodb.database)
                            .getCollection<BsonDocument>(
                                Node.instance.configProvider.config.mongodb.collectionPrefix +
                                    "nodeSnapshots"
                            )

                    val filter =
                        BsonDocument(
                            "name",
                            BsonString(Node.instance.configProvider.config.nodeName),
                        )

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

                    collection.replaceOne(
                        filter,
                        snapshot.toDocument(),
                        ReplaceOptions().upsert(true),
                    )

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
