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
            serviceStub.getByTask(getByTaskRequest { this.task = taskDefinition }).servicesList

        return serviceDefinition.map { Service.fromDefinition(it) }
    }

    override suspend fun getServicesByTask(task: Task): List<Service> {
        val serviceDefinition =
            serviceStub.getByTask(getByTaskRequest { this.task = task.toDefinition() }).servicesList

        return serviceDefinition.map { Service.fromDefinition(it) }
    }

    override suspend fun startService(service: Service): String {
        return serviceStub
            .startService(startServiceRequest { this.service = service.toDefinition() })
            .error
    }

    override suspend fun stopService(service: Service): String {
        return serviceStub
            .stopService(stopServiceRequest { this.service = service.toDefinition() })
            .error
    }

    override suspend fun deleteService(service: Service): String {
        return serviceStub
            .deleteService(deleteServiceRequest { this.service = service.toDefinition() })
            .error
    }

    override suspend fun restartService(service: Service): String {
        return serviceStub
            .restartService(restartServiceRequest { this.service = service.toDefinition() })
            .error
    }

    override suspend fun sendCommand(service: Service, command: String): Boolean {
        return serviceStub
            .sendCommand(
                sendCommandRequest {
                    this.service = service.toDefinition()
                    this.command = command
                }
            )
            .success
    }

    override suspend fun getLocalService(): Service? {
        return Service.fromDefinition(
            serviceStub
                .getByUuid(getByUuidRequest { this.uuid = Wrapper.SERVICE_UUID.toString() })
                .serviceOrNull ?: return null
        )
    }
}
