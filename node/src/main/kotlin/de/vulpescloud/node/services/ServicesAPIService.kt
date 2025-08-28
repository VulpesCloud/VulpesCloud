package de.vulpescloud.node.services

import build.buf.gen.vulpescloud.services.v1.*
import de.vulpescloud.api.services.Service
import de.vulpescloud.node.Node
import kotlinx.coroutines.flow.firstOrNull
import org.bson.BsonDocument
import org.bson.BsonString

class ServicesAPIService : ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineImplBase() {

    override suspend fun getAllServices(request: GetAllServicesRequest): GetAllServicesResponse {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "services"
                )

        val services = mutableListOf<ServiceDefinition>()

        collection.find().collect {
            services.add(Service.fromDocument(it.toBsonDocument()).toDefinition())
        }

        return GetAllServicesResponse.newBuilder().addAllServices(services).build()
    }

    override suspend fun getByName(request: GetByNameRequest): GetByNameResponse {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "services"
                )
        val filter = BsonDocument("name", BsonString(request.name))
        val serviceDoc = collection.find(filter).firstOrNull()
        val service = serviceDoc?.let { Service.fromDocument(it) }
        if (service == null) {
            return GetByNameResponse.newBuilder().build()
        }
        return GetByNameResponse.newBuilder().setService(service.toDefinition()).build()
    }

    override suspend fun getByUuid(request: GetByUuidRequest): GetByUuidResponse {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "services"
                )
        val filter = BsonDocument("uuid", BsonString(request.uuid.toString()))
        val serviceDoc = collection.find(filter).firstOrNull()
        val service = serviceDoc?.let { Service.fromDocument(it) }
        if (service == null) {
            return GetByUuidResponse.newBuilder().build()
        }
        return GetByUuidResponse.newBuilder().setService(service.toDefinition()).build()
    }
}
