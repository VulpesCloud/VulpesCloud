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

package org.vulpesstudios.vulpescloud.api.maintenance

import kotlinx.serialization.Serializable

/**
 * Content of the `vc_node_maintenance` VirtualConfig. This is the single source of truth for
 * whether a node is in maintenance mode, also while the node is offline.
 */
@Serializable
data class NodeMaintenanceConfig(
    /** Global default join permission. `null` or `""` = no restriction. */
    val joinPermission: String? = null,
    /** Whether services on maintenance nodes count towards `minOnlineServices`. */
    val countMaintenanceServices: Boolean = true,
    /** Whether a stop/exit during a running drain puts the node into maintenance. */
    val maintenanceOnStopDuringDrain: Boolean = true,
    val nodes: Map<String, NodeMaintenanceEntry> = emptyMap(),
) {
    fun isInMaintenance(nodeName: String): Boolean = nodes[nodeName]?.maintenance == true

    /**
     * Resolves the join permission for [nodeName]: the node entry wins if it is not `null`,
     * otherwise the global one is used. An empty string disables the permission.
     * Returns `null` when no restriction applies.
     */
    fun resolveJoinPermission(nodeName: String): String? {
        val effective = nodes[nodeName]?.joinPermission ?: joinPermission
        return effective?.takeIf { it.isNotEmpty() }
    }

    fun withMaintenance(nodeName: String, enabled: Boolean): NodeMaintenanceConfig {
        val entry = nodes[nodeName] ?: NodeMaintenanceEntry()
        return copy(nodes = nodes + (nodeName to entry.copy(maintenance = enabled)))
    }

    companion object {
        const val VIRTUAL_CONFIG_NAME = "vc_node_maintenance"

        /** Key in `NodeSnapshot.attributes` that mirrors the maintenance flag while a node is online. */
        const val ATTRIBUTE_KEY = "maintenance"
    }
}

@Serializable
data class NodeMaintenanceEntry(
    val maintenance: Boolean = false,
    /** `null` = use the global permission, `""` = no permission required on this node. */
    val joinPermission: String? = null,
)
