package de.vulpescloud.node.services

import build.buf.gen.vulpescloud.services.v1.*
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import kotlinx.coroutines.flow.firstOrNull
import org.bson.BsonDocument
import org.bson.BsonString
import org.slf4j.LoggerFactory

class ServicesAPIService : ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger("ServicesAPIService")

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

    override suspend fun prepareServiceByTask(
        request: PrepareServiceByTaskRequest
    ): PrepareServiceByTaskResponse {
        val task = Task.fromDefinition(request.task)

        val serviceFactory =
            Node.instance.serviceFactoryProvider.findServiceFactory(task.serviceFactoryName)
        if (serviceFactory == null) {
            throw IllegalArgumentException(
                "Unable to find ServiceFactory ${task.serviceFactoryName}"
            )
        }
        val service = serviceFactory.prepareService(task)

        return PrepareServiceByTaskResponse.newBuilder()
            .setService(service.service.toDefinition())
            .build()
    }

    override suspend fun prepareServiceByService(
        request: PrepareServiceByServiceRequest
    ): PrepareServiceByServiceResponse {
        val service = Service.fromDefinition(request.service)

        val serviceFactory =
            Node.instance.serviceFactoryProvider.findServiceFactory(service.task.serviceFactoryName)
        if (serviceFactory == null) {
            throw IllegalArgumentException(
                "Unable to find ServiceFactory ${service.task.serviceFactoryName}"
            )
        }
        val abstractService = serviceFactory.prepareService(service)

        return PrepareServiceByServiceResponse.newBuilder()
            .setService(abstractService.service.toDefinition())
            .build()
    }

    override suspend fun startService(request: StartServiceRequest): StartServiceResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = Node.instance.nodeServices.find { it.service.uuid == service.uuid }
        if (abstractService == null) {
            logger.error(
                "Unable to start Service ${service.task.name}-${service.orderedId} as it is not registered!"
            )
            return StartServiceResponse.newBuilder().build()
        }
        abstractService.start()

        return StartServiceResponse.newBuilder()
            .setService(abstractService.service.toDefinition())
            .build()
    }

    override suspend fun stopService(request: StopServiceRequest): StopServiceResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = Node.instance.nodeServices.find { it.service.uuid == service.uuid }
        if (abstractService == null) {
            logger.error(
                "Unable to stop Service ${service.task.name}-${service.orderedId} as it is not registered!"
            )
            return StopServiceResponse.newBuilder().build()
        }
        abstractService.stop()

        return StopServiceResponse.newBuilder()
            .setService(abstractService.service.toDefinition())
            .build()
    }

    override suspend fun restartService(request: RestartServiceRequest): RestartServiceResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = Node.instance.nodeServices.find { it.service.uuid == service.uuid }
        if (abstractService == null) {
            logger.error(
                "Unable to restart Service ${service.task.name}-${service.orderedId} as it is not registered!"
            )
            return RestartServiceResponse.newBuilder().build()
        }
        abstractService.restart()

        return RestartServiceResponse.newBuilder()
            .setService(abstractService.service.toDefinition())
            .build()
    }

    override suspend fun deleteService(request: DeleteServiceRequest): DeleteServiceResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = Node.instance.nodeServices.find { it.service.uuid == service.uuid }
        if (abstractService == null) {
            logger.error(
                "Unable to delete Service ${service.task.name}-${service.orderedId} as it is not registered!"
            )
            return DeleteServiceResponse.newBuilder().build()
        }
        abstractService.delete()

        return DeleteServiceResponse.newBuilder()
            .setService(abstractService.service.toDefinition())
            .build()
    }

    override suspend fun sendCommand(request: SendCommandRequest): SendCommandResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = Node.instance.nodeServices.find { it.service.uuid == service.uuid }
        if (abstractService == null) {
            logger.error(
                "Unable to send command to Service ${service.task.name}-${service.orderedId} as it is not registered!"
            )
            return SendCommandResponse.newBuilder().build()
        }
        abstractService.command(request.command)

        return SendCommandResponse.newBuilder().setSuccess(true).build()
    }
}
