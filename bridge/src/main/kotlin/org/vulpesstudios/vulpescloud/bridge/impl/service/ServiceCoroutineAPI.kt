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
import org.vulpesstudios.vulpescloud.bridge.ServiceAPI
import org.vulpesstudios.vulpescloud.wrapper.Wrapper
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
