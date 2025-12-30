package de.vulpescloud.node.virtualconfig

import build.buf.gen.vulpescloud.virtualconfig.v1.*
import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.annotations.RequiresPermission
import de.vulpescloud.node.utils.MongoUtils
import kotlinx.coroutines.flow.firstOrNull
import org.bson.BsonDocument
import org.bson.BsonString
import build.buf.gen.vulpescloud.virtualconfig.v1.VirtualConfig as VirtualConfigDefinition

class VirtualConfigServiceImpl :
    VirtualConfigServiceGrpcKt.VirtualConfigServiceCoroutineImplBase() {

    @RequiresPermission("virtualconfig.list")
    override suspend fun getAll(request: GetAllRequest): GetAllResponse {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "virtualconfigs"
                )

        val configs = mutableListOf<VirtualConfigDefinition>()

        collection.find().collect { configs.add(fromDocumentToDefinition(it.toBsonDocument())) }

        return getAllResponse { this.configs.addAll(configs) }
    }

    @RequiresPermission("virtualconfig.get")
    override suspend fun getByName(request: GetByNameRequest): GetByNameResponse {

        require(request.name.isNotEmpty()) { "Name must not be empty" }

        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "virtualconfigs"
                )
        val filter = BsonDocument("name", BsonString(request.name))
        val configDoc = collection.find(filter).firstOrNull()
        val config = configDoc?.let { fromDocumentToDefinition(it) }
        if (config == null) {
            return getByNameResponse {}
        }
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
            this.createdAt = System.currentTimeMillis()
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
