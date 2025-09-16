package de.vulpescloud.bridge.impl.service

import build.buf.gen.vulpescloud.services.v1.*
import build.buf.gen.vulpescloud.tasks.v1.taskOrNull
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.bridge.ServiceAPI
import de.vulpescloud.wrapper.Wrapper
import java.util.*

class ServiceCoroutineAPI : ServiceAPI.ServiceCoroutineAPI {

    private val serviceStub = Wrapper.instance.grpcClient.serviceAPI
    private val tasksStub = Wrapper.instance.grpcClient.tasksAPI

    override suspend fun getAllServices(): List<Service> {
        return serviceStub.getAllServices(getAllServicesRequest {}).servicesList.map {
            Service.fromDefinition(it)
        }
    }

    override suspend fun getServiceByName(name: String): Service? {
        val definition = serviceStub.getByName(getByNameRequest { this.name = name }).serviceOrNull

        return if (definition == null) null else Service.fromDefinition(definition)
    }

    override suspend fun getServiceByUUID(uuid: UUID): Service? {
        val definition =
            serviceStub.getByUuid(getByUuidRequest { this.uuid = uuid.toString() }).serviceOrNull

        return if (definition == null) null else Service.fromDefinition(definition)
    }

    override suspend fun getServicesByTask(task: String): List<Service> {
        val taskDefinition =
            tasksStub
                .getByName(build.buf.gen.vulpescloud.tasks.v1.getByNameRequest { this.name = task })
                .taskOrNull

        if (taskDefinition == null) {
            return emptyList()
        }

        val serviceDefinition =
            serviceStub
                .getByTask(
                    build.buf.gen.vulpescloud.services.v1.getByTaskRequest {
                        this.task = taskDefinition
                    }
                )
                .servicesList

        return serviceDefinition.map { Service.fromDefinition(it) }
    }

    override suspend fun getServicesByTask(task: Task): List<Service> {
        val serviceDefinition =
            serviceStub
                .getByTask(
                    build.buf.gen.vulpescloud.services.v1.getByTaskRequest {
                        this.task = task.toDefinition()
                    }
                )
                .servicesList

        return serviceDefinition.map { Service.fromDefinition(it) }
    }

    override suspend fun prepareService(service: Service): Service? {
        return Service.fromDefinition(
            serviceStub
                .createService(createServiceRequest { this.task = service.task.toDefinition() })
                .serviceOrNull ?: return null
        )

        // TODO: Add Protobuf to prepare service from service definition
    }

    override suspend fun prepareService(task: Task): Service? {
        return Service.fromDefinition(
            serviceStub
                .createService(createServiceRequest { this.task = task.toDefinition() })
                .serviceOrNull ?: return null
        )
    }

    override suspend fun startService(service: Service): Service? {
        return Service.fromDefinition(
            serviceStub
                .startService(startServiceRequest { this.service = service.toDefinition() })
                .serviceOrNull ?: return null
        )
    }

    override suspend fun stopService(service: Service): Service? {
        return Service.fromDefinition(
            serviceStub
                .stopService(stopServiceRequest { this.service = service.toDefinition() })
                .serviceOrNull ?: return null
        )
    }

    override suspend fun deleteService(service: Service): Service? {
        return Service.fromDefinition(
            serviceStub
                .deleteService(deleteServiceRequest { this.service = service.toDefinition() })
                .serviceOrNull ?: return null
        )
    }

    override suspend fun restartService(service: Service): Service? {
        return Service.fromDefinition(
            serviceStub
                .restartService(restartServiceRequest { this.service = service.toDefinition() })
                .serviceOrNull ?: return null
        )
    }

    override suspend fun sendCommand(service: Service, command: String): Boolean {
        return false
        // TODO: update Protobuf to return boolean
    }
}
