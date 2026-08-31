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

package org.vulpesstudios.vulpescloud.node.virtualconfig

import build.buf.gen.vulpescloud.virtualconfig.v1.*
import kotlinx.serialization.json.Json
import org.vulpesstudios.vulpescloud.api.virtualconfig.VirtualConfig
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.grpc.security.annotations.RequiresPermission
import org.vulpesstudios.vulpescloud.node.utils.MongoUtils

class VirtualConfigServiceImpl :
    VirtualConfigServiceGrpcKt.VirtualConfigServiceCoroutineImplBase() {

    private val virtualConfigDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("virtualconfigs")
    }

    @RequiresPermission("virtualconfig.list")
    override suspend fun getAll(request: GetAllRequest): GetAllResponse {
        val configs =
            virtualConfigDatabase
                .getAll()
                .map { Json.decodeFromJsonElement(VirtualConfig.serializer(), it) }
                .map { it.toDefinition() }

        return getAllResponse { this.configs.addAll(configs) }
    }

    @RequiresPermission("virtualconfig.get")
    override suspend fun getByName(request: GetByNameRequest): GetByNameResponse {

        require(request.name.isNotEmpty()) { "Name must not be empty" }

        val config =
            Json.decodeFromJsonElement(
                    VirtualConfig.serializer(),
                    virtualConfigDatabase.get(request.name)
                        ?: return GetByNameResponse.newBuilder().build(),
                )
                .toDefinition()
        return getByNameResponse { this.config = config }
    }

    @RequiresPermission("virtualconfig.create")
    override suspend fun createVirtualConfig(
        request: CreateVirtualConfigRequest
    ): CreateVirtualConfigResponse {

        require(request.name.isNotEmpty()) { "Name must not be empty" }
        require(request.config.isNotEmpty()) { "Config must not be empty" }

        val config = virtualConfig {
            this.config = request.config
            this.name = request.name
            this.createdAt = System.currentTimeMillis()
            this.lastUpdatedAt = System.currentTimeMillis()
        }

        MongoUtils.nothingOrInsertVirtualConfig(config)

        return createVirtualConfigResponse { this.config = config }
    }

    @RequiresPermission("virtualconfig.delete")
    override suspend fun deleteVirtualConfig(
        request: DeleteVirtualConfigRequest
    ): DeleteVirtualConfigResponse {

        require(request.name.isNotEmpty()) { "Name must not be empty" }

        MongoUtils.deleteVirtualConfig(virtualConfig { this.name = request.name })

        return deleteVirtualConfigResponse { this.success = true }
    }

    @RequiresPermission("virtualconfig.update")
    override suspend fun updateVirtualConfig(
        request: UpdateVirtualConfigRequest
    ): UpdateVirtualConfigResponse {

        require(request.name.isNotEmpty()) { "Name must not be empty" }
        require(request.config.isNotEmpty()) { "Config must not be empty" }

        val config = virtualConfig {
            this.config = request.config
            this.name = request.name
            this.lastUpdatedAt = System.currentTimeMillis()
        }

        MongoUtils.updateOrInsertVirtualConfig(config)

        return updateVirtualConfigResponse { this.config = config }
    }

}
