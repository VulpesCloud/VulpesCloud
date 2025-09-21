package de.vulpescloud.node.services

import build.buf.gen.vulpescloud.services.v1.GetAllServicesRequest
import com.google.protobuf.Timestamp
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
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
                Node.instance.configProvider.config.serviceBindAdress
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
