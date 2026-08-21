package de.vulpescloud.node.virtualconfig

import build.buf.gen.vulpescloud.virtualconfig.v1.*
import de.vulpescloud.api.virtualconfig.VirtualConfig
import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.annotations.RequiresPermission
import de.vulpescloud.node.utils.MongoUtils
import kotlinx.serialization.json.Json
import org.bson.BsonDocument
import build.buf.gen.vulpescloud.virtualconfig.v1.VirtualConfig as VirtualConfigDefinition

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

    private fun fromDocumentToDefinition(document: BsonDocument): VirtualConfigDefinition {
        return virtualConfig {
            this.name = document.getString("name").value
            this.createdAt = document.getInt64("createdAt").value
            this.lastUpdatedAt = document.getInt64("lastUpdatedAt").value
            this.config = document.getString("config").value
        }
    }
}
