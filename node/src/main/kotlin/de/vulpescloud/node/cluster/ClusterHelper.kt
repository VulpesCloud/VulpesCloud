package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.NodeSnapshot
import de.vulpescloud.node.Node
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

object ClusterHelper {

    private val snapshotDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("nodeSnapshots")
    }
    private val json = Json

    suspend fun getLocalNodeSnapshot(): NodeSnapshot {
        val jsonElement = snapshotDatabase.get(Node.instance.configProvider.config.nodeName)
        return if (jsonElement != null) {
            json.decodeFromJsonElement(jsonElement)
        } else {
            NodeSnapshotUpdater.generateSnapshot()
        }
    }
}
