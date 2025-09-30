package de.vulpescloud.bridge.impl.service

import build.buf.gen.vulpescloud.services.v1.*
import build.buf.gen.vulpescloud.tasks.v1.taskOrNull
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.bridge.FutureHelper.toCompletableFuture
import de.vulpescloud.bridge.ServiceAPI
import de.vulpescloud.wrapper.Wrapper
import java.util.*
import java.util.concurrent.CompletableFuture

class ServiceFutureAPI : ServiceAPI.ServiceFutureAPI {

    private val serviceStub = Wrapper.instance.grpcClient.futureServiceAPI
    private val tasksStub = Wrapper.instance.grpcClient.futureTasksAPI

    override fun getAllServices(): CompletableFuture<List<Service>> {
        return serviceStub
            .getAllServices(getAllServicesRequest {})
            .toCompletableFuture()
            .thenApply { it.servicesList.map { service -> Service.fromDefinition(service) } }
    }

    override fun getServiceByName(name: String): CompletableFuture<Service?> {
        return serviceStub
            .getByName(getByNameRequest { this.name = name })
            .toCompletableFuture()
            .thenApply { Service.fromDefinition(it.serviceOrNull ?: return@thenApply null) }
    }

    override fun getServiceByUUID(uuid: UUID): CompletableFuture<Service?> {
        return serviceStub
            .getByUuid(getByUuidRequest { this.uuid = uuid.toString() })
            .toCompletableFuture()
            .thenApply { Service.fromDefinition(it.serviceOrNull ?: return@thenApply null) }
    }

    override fun getServicesByTask(task: String): CompletableFuture<List<Service>> {
        return tasksStub
            .getByName(build.buf.gen.vulpescloud.tasks.v1.getByNameRequest { this.name = task })
            .toCompletableFuture()
            .thenCompose { getByNameResponse ->
                val foundTask = getByNameResponse.taskOrNull
                if (foundTask == null) {
                    CompletableFuture.completedFuture(emptyList<Service>())
                } else {
                    serviceStub
                        .getByTask(getByTaskRequest { this.task = foundTask })
                        .toCompletableFuture()
                        .thenApply { response ->
                            response.servicesList.map { service -> Service.fromDefinition(service) }
                        }
                }
            }
    }

    override fun getServicesByTask(task: Task): CompletableFuture<List<Service>> {
        return serviceStub
            .getByTask(getByTaskRequest { this.task = task.toDefinition() })
            .toCompletableFuture()
            .thenApply { response ->
                response.servicesList.map { service -> Service.fromDefinition(service) }
            }
    }

//    override fun prepareService(service: Service): CompletableFuture<Service?> {
//        return serviceStub
//            .prepareServiceByService(
//                prepareServiceByServiceRequest { this.service = service.toDefinition() }
//            )
//            .toCompletableFuture()
//            .thenApply { Service.fromDefinition(it.serviceOrNull ?: return@thenApply null) }
//    }

    override fun prepareService(task: Task): CompletableFuture<Service?> {
        return serviceStub
            .prepareServiceByTask(prepareServiceByTaskRequest { this.task = task.toDefinition() })
            .toCompletableFuture()
            .thenApply { Service.fromDefinition(it.serviceOrNull ?: return@thenApply null) }
    }

    override fun startService(service: Service): CompletableFuture<Service?> {
        return serviceStub
            .startService(startServiceRequest { this.service = service.toDefinition() })
            .toCompletableFuture()
            .thenApply { Service.fromDefinition(it.serviceOrNull ?: return@thenApply null) }
    }

    override fun stopService(service: Service): CompletableFuture<Service?> {
        return serviceStub
            .stopService(stopServiceRequest { this.service = service.toDefinition() })
            .toCompletableFuture()
            .thenApply { Service.fromDefinition(it.serviceOrNull ?: return@thenApply null) }
    }

    override fun deleteService(service: Service): CompletableFuture<Service?> {
        return serviceStub
            .deleteService(deleteServiceRequest { this.service = service.toDefinition() })
            .toCompletableFuture()
            .thenApply { Service.fromDefinition(it.serviceOrNull ?: return@thenApply null) }
    }

    override fun restartService(service: Service): CompletableFuture<Service?> {
        return serviceStub
            .restartService(restartServiceRequest { this.service = service.toDefinition() })
            .toCompletableFuture()
            .thenApply { Service.fromDefinition(it.serviceOrNull ?: return@thenApply null) }
    }

    override fun sendCommand(service: Service, command: String): CompletableFuture<Boolean> {
        return serviceStub
            .sendCommand(
                sendCommandRequest {
                    this.service = service.toDefinition()
                    this.command = command
                }
            )
            .toCompletableFuture()
            .thenApply { it.success }
    }

    override fun getLocalService(): CompletableFuture<Service?> {
        return serviceStub
            .getByUuid(getByUuidRequest { this.uuid = Wrapper.SERVICE_UUID.toString() })
            .toCompletableFuture()
            .thenApply { Service.fromDefinition(it.serviceOrNull ?: return@thenApply null) }
    }
}
