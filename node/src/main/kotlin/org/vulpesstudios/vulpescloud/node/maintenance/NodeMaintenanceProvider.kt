/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.maintenance

import build.buf.gen.vulpescloud.cluster.v2.NodeMaintenanceChangedEvent
import build.buf.gen.vulpescloud.cluster.v2.nodeMaintenanceChangedEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.maintenance.NodeMaintenanceConfig
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.cluster.ClusterHelper
import org.vulpesstudios.vulpescloud.node.cluster.NodeSnapshotUpdater
import org.vulpesstudios.vulpescloud.node.event.EventsService

/**
 * Access to the node maintenance state. The VirtualConfig `vc_node_maintenance` is the single
 * source of truth; while a node is online the flag is mirrored to
 * `NodeSnapshot.attributes["maintenance"]`.
 *
 * Note: the VirtualConfig has no compare-and-set, so two nodes writing this config at the very same
 * moment could overwrite each other. The local [mutex] only serialises writes of this node.
 */
class NodeMaintenanceProvider {

    private val logger = LoggerFactory.getLogger("NodeMaintenanceProvider")
    private val mutex = Mutex()

    @Volatile private var cached: NodeMaintenanceConfig? = null
    @Volatile private var cachedAt: Long = 0L

    private val localNodeName: String
        get() = Node.instance.configProvider.config.nodeName

    fun subscribeToEvents() {
        EventsService.subscribe<NodeMaintenanceChangedEvent> { event ->
            if (event.snapshot.name == localNodeName) {
                cached = null
                syncLocalAttribute(forceGet = true)
            }
        }
    }

    /** Loads the config. Uses a short-lived cache unless [forceGet] is set. */
    suspend fun getConfig(forceGet: Boolean = false): NodeMaintenanceConfig {
        val known = cached
        if (
            !forceGet && known != null && System.currentTimeMillis() - cachedAt < CACHE_TTL_MILLIS
        ) {
            return known
        }

        val loaded =
            try {
                Node.instance.virtualConfigProvider.getCustomConfigObject<NodeMaintenanceConfig>(
                    NodeMaintenanceConfig.VIRTUAL_CONFIG_NAME,
                    forceGet = true,
                )
            } catch (e: Exception) {
                logger.error("Failed to load ${NodeMaintenanceConfig.VIRTUAL_CONFIG_NAME}", e)
                // keep the last known state instead of suddenly leaving maintenance
                return known ?: NodeMaintenanceConfig()
            }

        val config = loaded ?: NodeMaintenanceConfig()
        cached = config
        cachedAt = System.currentTimeMillis()
        return config
    }

    suspend fun isInMaintenance(nodeName: String, forceGet: Boolean = false): Boolean =
        getConfig(forceGet).isInMaintenance(nodeName)

    /**
     * Enables or disables maintenance for [nodeName] (works for offline nodes too). Returns `true`
     * if the state actually changed.
     */
    suspend fun setMaintenance(nodeName: String, enabled: Boolean): Boolean {
        val changed = mutex.withLock {
            val current = getConfig(forceGet = true)
            if (current.isInMaintenance(nodeName) == enabled) return@withLock false

            val updated = current.withMaintenance(nodeName, enabled)
            Node.instance.virtualConfigProvider.updateCustomConfig(
                NodeMaintenanceConfig.VIRTUAL_CONFIG_NAME,
                updated,
            )
            cached = updated
            cachedAt = System.currentTimeMillis()
            true
        }

        if (changed) {
            if (nodeName == localNodeName) syncLocalAttribute()
            publishChanged(nodeName, enabled)
        }
        return changed
    }

    private suspend fun publishChanged(nodeName: String, enabled: Boolean) {
        val nodeSnapshot =
            ClusterHelper.getAllNodeSnapshots().firstOrNull { it.name == nodeName } ?: return
        EventsService.publish(
            nodeMaintenanceChangedEvent {
                this.snapshot = nodeSnapshot.toDefinition()
                this.enabled = enabled
            },
            true,
        )
    }

    /**
     * Mirrors the persisted maintenance flag of this node into the node attributes. Returns `true`
     * if the attribute changed. Does not refresh the snapshot itself.
     */
    suspend fun applyLocalAttribute(forceGet: Boolean = false): Boolean {
        val enabled = isInMaintenance(localNodeName, forceGet)
        val attributes = Node.instance.clusterProvider.currentAttributes
        return if (enabled) {
            attributes.put(NodeMaintenanceConfig.ATTRIBUTE_KEY, "true") != "true"
        } else {
            attributes.remove(NodeMaintenanceConfig.ATTRIBUTE_KEY) != null
        }
    }

    /** Like [applyLocalAttribute], but publishes a new node snapshot when the attribute changed. */
    suspend fun syncLocalAttribute(forceGet: Boolean = false) {
        if (applyLocalAttribute(forceGet)) {
            NodeSnapshotUpdater.updateLocalNodeSnapshot()
        }
    }

    companion object {
        private const val CACHE_TTL_MILLIS = 2_000L
    }
}
