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

package org.vulpesstudios.vulpescloud.bridge.impl.service

import build.buf.gen.vulpescloud.services.v1.*
import build.buf.gen.vulpescloud.tasks.v1.taskOrNull
import org.vulpesstudios.vulpescloud.api.services.Service
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.bridge.FutureHelper.toCompletableFuture
import org.vulpesstudios.vulpescloud.bridge.ServiceAPI
import org.vulpesstudios.vulpescloud.wrapper.Wrapper
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

    override fun startService(service: Service): CompletableFuture<String> {
        return serviceStub
            .startService(startServiceRequest { this.service = service.toDefinition() })
            .toCompletableFuture()
            .thenApply { it.error }
    }

    override fun stopService(service: Service): CompletableFuture<String> {
        return serviceStub
            .stopService(stopServiceRequest { this.service = service.toDefinition() })
            .toCompletableFuture()
            .thenApply { it.error }
    }

    override fun deleteService(service: Service): CompletableFuture<String> {
        return serviceStub
            .deleteService(deleteServiceRequest { this.service = service.toDefinition() })
            .toCompletableFuture()
            .thenApply { it.error }
    }

    override fun restartService(service: Service): CompletableFuture<String> {
        return serviceStub
            .restartService(restartServiceRequest { this.service = service.toDefinition() })
            .toCompletableFuture()
            .thenApply { it.error }
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
