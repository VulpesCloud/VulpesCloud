package de.vulpescloud.node.services

import build.buf.gen.vulpescloud.services.v1.*
import com.google.protobuf.Timestamp
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.services.impl.docker.DockerService
import de.vulpescloud.node.services.impl.docker.DockerServiceFactory
import de.vulpescloud.node.services.impl.local.LocalService
import de.vulpescloud.node.services.impl.local.LocalServiceFactory
import kotlinx.coroutines.flow.firstOrNull
import org.bson.BsonDocument
import org.bson.BsonString
import java.util.UUID

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

    override suspend fun createService(request: CreateServiceRequest): CreateServiceResponse {
        val task = Task.fromDefinition(request.task)

        val factory = if (task.serviceFactoryName.contains("docker", true)) {
            DockerServiceFactory()
        } else {
            LocalServiceFactory()
        }

        val service = Service(
            task,
            UUID.randomUUID(),
            factory.generateOrderedId(task),
            factory.detectServicePort(task),
            task.preferredNode,
            0,
            Timestamp.newBuilder().build(),
            ServiceStates.PREPARED,
        )

        return CreateServiceResponse.newBuilder().setService(service.toDefinition()).build()
    }

    override suspend fun startService(request: StartServiceRequest): StartServiceResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = if (service.task.serviceFactoryName.contains("docker", true)) {
            DockerService(service)
        } else {
            LocalService(service)
        }
        abstractService.start()

        return StartServiceResponse.newBuilder().setService(abstractService.service.toDefinition()).build()
    }

    override suspend fun stopService(request: StopServiceRequest): StopServiceResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = if (service.task.serviceFactoryName.contains("docker", true)) {
            DockerService(service)
        } else {
            LocalService(service)
        }
        abstractService.stop()

        return StopServiceResponse.newBuilder().setService(abstractService.service.toDefinition()).build()
    }

    override suspend fun restartService(request: RestartServiceRequest): RestartServiceResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = if (service.task.serviceFactoryName.contains("docker", true)) {
            DockerService(service)
        } else {
            LocalService(service)
        }

        abstractService.restart()

        return RestartServiceResponse.newBuilder().setService(abstractService.service.toDefinition()).build()
    }

    override suspend fun deleteService(request: DeleteServiceRequest): DeleteServiceResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = if (service.task.serviceFactoryName.contains("docker", true)) {
            DockerService(service)
        } else {
            LocalService(service)
        }

        abstractService.delete()

        return DeleteServiceResponse.newBuilder().setService(abstractService.service.toDefinition()).build()
    }
}
