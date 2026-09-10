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

package org.vulpesstudios.vulpescloud.node.services

import build.buf.gen.vulpescloud.services.v1.GetAllServicesRequest
import com.google.protobuf.Timestamp
import org.vulpesstudios.vulpescloud.api.services.Service
import org.vulpesstudios.vulpescloud.api.services.ServiceStates
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.node.Node
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.*

abstract class AbstractServiceFactory {

    abstract val factoryName: String

    suspend fun prepareService(task: Task): AbstractService {
        return prepareService(
            Service(
                task,
                UUID.randomUUID(),
                generateOrderedId(task),
                detectServicePort(task),
                "",
                0,
                Timestamp.newBuilder().build(),
                ServiceStates.UNKNOWN,
                Node.instance.configProvider.config.serviceBindAdress,
            )
        )
    }

    abstract suspend fun prepareService(service: Service): AbstractService

    suspend fun generateOrderedId(task: Task): Int {
        val services =
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(GetAllServicesRequest.newBuilder().build())
                .servicesList
                .filter { it.task.name == task.name }

        val existingIds = services.map { it.orderedId }.toSet()

        return (1..Int.MAX_VALUE).first { id -> id !in existingIds }
    }

    suspend fun findNextAvailableOrderedId(task: Task, startOrderedId: Int): Int {
        val services =
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(GetAllServicesRequest.newBuilder().build())
                .servicesList
                .filter { it.task.name == task.name }
        val existingIds = services.map { it.orderedId }.toSet()
        return (startOrderedId..Int.MAX_VALUE).first { id -> id !in existingIds }
    }

    suspend fun detectServicePort(task: Task): Int {
        var serverPort: Long = task.startPort

        while (isUsed(serverPort)) {
            serverPort++
        }

        return serverPort.toInt()
    }

    suspend fun isUsed(port: Long): Boolean {
        for (service in
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(GetAllServicesRequest.newBuilder().build())
                .servicesList) {
            if (service.port.toLong() == port) {
                return true
            }
        }
        try {
            ServerSocket().use { testSocket ->
                testSocket.bind(InetSocketAddress(port.toInt()))
                return false
            }
        } catch (_: Exception) {
            return true
        }
    }
}
