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

import build.buf.gen.vulpescloud.maintenance.v1.*
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.cluster.ClusterHelper
import org.vulpesstudios.vulpescloud.node.grpc.security.annotations.RequiresPermission

class NodeMaintenanceAPIServiceImpl :
    NodeMaintenanceAPIServiceGrpcKt.NodeMaintenanceAPIServiceCoroutineImplBase() {
    @RequiresPermission("maintenance.set", ["node=nodeName"])
    override suspend fun setNodeMaintenance(
        request: SetNodeMaintenanceRequest
    ): SetNodeMaintenanceResponse {
        return try {
            Node.instance.nodeMaintenanceProvider.setMaintenance(request.nodeName, request.enabled)
            setNodeMaintenanceResponse { success = true }
        } catch (e: Exception) {
            setNodeMaintenanceResponse { error = "maintenance_update_failed: ${e.message}" }
        }
    }

    @RequiresPermission("maintenance.list")
    override suspend fun listNodeMaintenance(
        request: ListNodeMaintenanceRequest
    ): ListNodeMaintenanceResponse {
        val config = Node.instance.nodeMaintenanceProvider.getConfig(forceGet = true)
        return listNodeMaintenanceResponse {
            nodes.addAll(
                ClusterHelper.getAllNodeSnapshots().map { snapshot ->
                    nodeMaintenanceState {
                        nodeName = snapshot.name
                        enabled = config.isInMaintenance(snapshot.name)
                    }
                }
            )
        }
    }
}
