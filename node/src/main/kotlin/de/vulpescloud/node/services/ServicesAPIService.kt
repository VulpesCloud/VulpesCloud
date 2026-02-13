package de.vulpescloud.node.services

import build.buf.gen.vulpescloud.services.v1.*
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.annotations.RequiresPermission
import de.vulpescloud.node.utils.MongoUtils
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class ServicesAPIService : ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger("ServicesAPIService")
    private val servicesDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("services")
    }

    @RequiresPermission("services.list")
    override suspend fun getAllServices(request: GetAllServicesRequest): GetAllServicesResponse {
        val services =
            servicesDatabase
                .getAll()
                .map { Json.decodeFromJsonElement(Service.serializer(), it) }
                .map { it.toDefinition() }

        return GetAllServicesResponse.newBuilder().addAllServices(services).build()
    }

    @RequiresPermission("services.get")
    override suspend fun getByName(request: GetByNameRequest): GetByNameResponse {
        val service =
            getAllServices(getAllServicesRequest {}).servicesList.find {
                "${it.task.name}-${it.orderedId}" == request.name
            } ?: return GetByNameResponse.newBuilder().build()
        return GetByNameResponse.newBuilder().setService(service).build()
    }

    @RequiresPermission("services.get")
    override suspend fun getByUuid(request: GetByUuidRequest): GetByUuidResponse {
        val service =
            servicesDatabase.get(request.uuid)?.let {
                Json.decodeFromJsonElement(Service.serializer(), it)
            } ?: return GetByUuidResponse.newBuilder().build()
        return GetByUuidResponse.newBuilder().setService(service.toDefinition()).build()
    }

    @RequiresPermission("services.prepare")
    override suspend fun prepareServiceByTask(
        request: PrepareServiceByTaskRequest
    ): PrepareServiceByTaskResponse {
        val task = Task.fromDefinition(request.task)

        val serviceFactory =
            Node.instance.serviceFactoryProvider.findServiceFactory(task.serviceFactoryName)
                ?: throw IllegalArgumentException(
                    "Unable to find ServiceFactory ${task.serviceFactoryName}"
                )
        val service = serviceFactory.prepareService(task)

        return PrepareServiceByTaskResponse.newBuilder()
            .setService(service.service.toDefinition())
            .build()
    }

    //    override suspend fun prepareServiceByService(
    //        request: PrepareServiceByServiceRequest
    //    ): PrepareServiceByServiceResponse {
    //        val service = Service.fromDefinition(request.service)
    //
    //        val serviceFactory =
    //
    // Node.instance.serviceFactoryProvider.findServiceFactory(service.task.serviceFactoryName)
    //        if (serviceFactory == null) {
    //            throw IllegalArgumentException(
    //                "Unable to find ServiceFactory ${service.task.serviceFactoryName}"
    //            )
    //        }
    //        val abstractService = serviceFactory.prepareService(service)
    //
    //        return PrepareServiceByServiceResponse.newBuilder()
    //            .setService(abstractService.service.toDefinition())
    //            .build()
    //    }

    @RequiresPermission("services.start")
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

    @RequiresPermission("services.stop")
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

    @RequiresPermission("services.restart")
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

    @RequiresPermission("services.delete")
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

    @RequiresPermission("services.sendCommand")
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

    override suspend fun updatePlayerCount(
        request: UpdatePlayerCountRequest
    ): UpdatePlayerCountResponse {
        val service = Service.fromDefinition(request.service)

        MongoUtils.updateService(service.copy(playerCount = request.playerCount))

        return UpdatePlayerCountResponse.newBuilder().build()
    }
}
