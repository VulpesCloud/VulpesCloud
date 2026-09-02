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

import build.buf.gen.vulpescloud.events.v1.ServiceLogEvent
import build.buf.gen.vulpescloud.services.v1.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.cluster.NodeState
import org.vulpesstudios.vulpescloud.api.services.Service
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.cluster.ClusterHelper
import org.vulpesstudios.vulpescloud.node.event.EventsService
import org.vulpesstudios.vulpescloud.node.grpc.security.AuthClientInterceptor
import org.vulpesstudios.vulpescloud.node.grpc.security.annotations.RequiresPermission
import org.vulpesstudios.vulpescloud.node.utils.PropertyUtils.isLoggingRedirects
import java.util.concurrent.ConcurrentHashMap

@Suppress("LoggingSimilarMessage", "DuplicatedCode")
class ServicesAPIService : ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger("ServicesAPIService")

    override suspend fun getServicesOfNode(
        request: GetServicesOfNodeRequest
    ): GetServicesOfNodeResponse {
        if (Node.instance.configProvider.config.nodeName != request.nodeName) {
            val correctNode =
                Node.instance.clusterProvider.remoteNodes.find {
                    it.endpoint.name == request.nodeName
                }
            if (correctNode?.channel == null) {
                return GetServicesOfNodeResponse.newBuilder().build()
            }

            val stub =
                ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(correctNode.channel!!)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))

            return stub.getServicesOfNode(request)
        }

        return GetServicesOfNodeResponse.newBuilder()
            .addAllServices(Node.instance.nodeServices.map { it.service.toDefinition() })
            .build()
    }

    @RequiresPermission("services.getAll")
    override suspend fun getAllServices(request: GetAllServicesRequest): GetAllServicesResponse {
        val services = mutableListOf<ServiceDefinition>()
        Node.instance.clusterProvider.remoteNodes
            .filter {
                it.channel != null && it.getSnapshot().state == NodeState.ONLINE ||
                    it.getSnapshot().state == NodeState.DRAINING
            }
            .forEach { node ->
                val stub = ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(node.channel!!)
                services +=
                    stub
                        .getServicesOfNode(
                            getServicesOfNodeRequest { nodeName = node.endpoint.name }
                        )
                        .servicesList
            }

        return GetAllServicesResponse.newBuilder().addAllServices(services).build()
    }

    private suspend fun getAllService(): List<ServiceDefinition> {
        return getAllServices(getAllServicesRequest {}).servicesList
    }

    @RequiresPermission("services.get")
    override suspend fun getByName(request: GetByNameRequest): GetByNameResponse {
        val service =
            getAllService()
                .map { Service.fromDefinition(it) }
                .find { "${it.task.name}-${it.orderedId}" == request.name }

        return if (service != null) {
            GetByNameResponse.newBuilder().setService(service.toDefinition()).build()
        } else {
            GetByNameResponse.newBuilder().build()
        }
    }

    @RequiresPermission("services.get")
    override suspend fun getByUuid(request: GetByUuidRequest): GetByUuidResponse {
        val service =
            getAllService().find { it.uuid == request.uuid }
                ?: return GetByUuidResponse.getDefaultInstance()

        return GetByUuidResponse.newBuilder().setService(service).build()
    }

    @RequiresPermission("services.start")
    override suspend fun startService(request: StartServiceRequest): StartServiceResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = Node.instance.nodeServices.find { it.service.uuid == service.uuid }
        if (abstractService == null) {
            if (isLoggingRedirects()) {
                logger.warn(
                    "Service ${service.task.name}-${service.orderedId} is not registered on this node, trying to notify responsible node!"
                )
            }
            val correctNode =
                Node.instance.clusterProvider.remoteNodes.find { it.endpoint.name == service.node }
            if (correctNode?.endpoint?.name == ClusterHelper.getLocalNodeSnapshot().name) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Service ${service.task.name}-${service.orderedId} is not registered on this node, but assigned to this node! This might happen if the Node shuts down without removing the service, delete it manually in MongoDB (Shutting down the Cluster is recommended when doing this)!"
                    )
                }
            }

            if (correctNode == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to start Service ${service.task.name}-${service.orderedId} as it is not registered in this node and the responsible node was not found!"
                    )
                }
                return StartServiceResponse.newBuilder()
                    .setSuccess(false)
                    .setError("node.notFound")
                    .build()
            }

            if (correctNode.channel == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to start Service ${service.task.name}-${service.orderedId} as the responsible node has no channel!"
                    )
                }
                return StartServiceResponse.newBuilder()
                    .setSuccess(false)
                    .setError("node.channel.null")
                    .build()
            }

            val stub =
                ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(correctNode.channel!!)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))

            stub.startService(request)

            return StartServiceResponse.newBuilder().setSuccess(true).build()
        }

        abstractService.start()

        return StartServiceResponse.newBuilder().setSuccess(true).build()
    }

    @RequiresPermission("services.stop")
    override suspend fun stopService(request: StopServiceRequest): StopServiceResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = Node.instance.nodeServices.find { it.service.uuid == service.uuid }
        if (abstractService == null) {
            if (isLoggingRedirects()) {
                logger.warn(
                    "Service ${service.task.name}-${service.orderedId} is not registered on this node, trying to notify responsible node!"
                )
            }
            val correctNode =
                Node.instance.clusterProvider.remoteNodes.find { it.endpoint.name == service.node }
            if (correctNode?.endpoint?.name == ClusterHelper.getLocalNodeSnapshot().name) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Service ${service.task.name}-${service.orderedId} is not registered on this node, but assigned to this node! This might happen if the Node shuts down without removing the service, delete it manually in MongoDB (Shutting down the Cluster is recommended when doing this)!"
                    )
                }
            }

            if (correctNode == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to stop Service ${service.task.name}-${service.orderedId} as it is not registered in this node and the responsible node was not found!"
                    )
                }
                return StopServiceResponse.newBuilder()
                    .setSuccess(false)
                    .setError("node.notFound")
                    .build()
            }

            if (correctNode.channel == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to stop Service ${service.task.name}-${service.orderedId} as the responsible node has no channel!"
                    )
                }
                return StopServiceResponse.newBuilder()
                    .setSuccess(false)
                    .setError("node.channel.null")
                    .build()
            }

            val stub =
                ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(correctNode.channel!!)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))

            return stub.stopService(request)
        }

        abstractService.stop()

        return StopServiceResponse.newBuilder().setSuccess(true).build()
    }

    @RequiresPermission("services.restart")
    override suspend fun restartService(request: RestartServiceRequest): RestartServiceResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = Node.instance.nodeServices.find { it.service.uuid == service.uuid }
        if (abstractService == null) {
            if (isLoggingRedirects()) {
                logger.warn(
                    "Service ${service.task.name}-${service.orderedId} is not registered on this node, trying to notify responsible node!"
                )
            }
            val correctNode =
                Node.instance.clusterProvider.remoteNodes.find { it.endpoint.name == service.node }
            if (correctNode?.endpoint?.name == ClusterHelper.getLocalNodeSnapshot().name) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Service ${service.task.name}-${service.orderedId} is not registered on this node, but assigned to this node! This might happen if the Node shuts down without removing the service, delete it manually in MongoDB (Shutting down the Cluster is recommended when doing this)!"
                    )
                }
            }

            if (correctNode == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to restart Service ${service.task.name}-${service.orderedId} as it is not registered in this node and the responsible node was not found!"
                    )
                }
                return RestartServiceResponse.newBuilder()
                    .setSuccess(false)
                    .setError("node.notFound")
                    .build()
            }

            if (correctNode.channel == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to restart Service ${service.task.name}-${service.orderedId} as the responsible node has no channel!"
                    )
                }
                return RestartServiceResponse.newBuilder()
                    .setSuccess(false)
                    .setError("node.channel.null")
                    .build()
            }

            val stub =
                ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(correctNode.channel!!)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))

            return stub.restartService(request)
        }

        abstractService.restart()

        return RestartServiceResponse.newBuilder().setSuccess(true).build()
    }

    @RequiresPermission("services.delete")
    override suspend fun deleteService(request: DeleteServiceRequest): DeleteServiceResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = Node.instance.nodeServices.find { it.service.uuid == service.uuid }
        if (abstractService == null) {
            if (isLoggingRedirects()) {
                logger.warn(
                    "Service ${service.task.name}-${service.orderedId} is not registered on this node, trying to notify responsible node!"
                )
            }
            val correctNode =
                Node.instance.clusterProvider.remoteNodes.find { it.endpoint.name == service.node }
            if (correctNode?.endpoint?.name == ClusterHelper.getLocalNodeSnapshot().name) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Service ${service.task.name}-${service.orderedId} is not registered on this node, but assigned to this node! This might happen if the Node shuts down without removing the service, delete it manually in MongoDB (Shutting down the Cluster is recommended when doing this)!"
                    )
                }
            }

            if (correctNode == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to delete Service ${service.task.name}-${service.orderedId} as it is not registered in this node and the responsible node was not found!"
                    )
                }
                return DeleteServiceResponse.newBuilder()
                    .setSuccess(false)
                    .setError("node.notFound")
                    .build()
            }

            if (correctNode.channel == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to delete Service ${service.task.name}-${service.orderedId} as the responsible node has no channel!"
                    )
                }
                return DeleteServiceResponse.newBuilder()
                    .setSuccess(false)
                    .setError("node.channel.null")
                    .build()
            }

            val stub =
                ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(correctNode.channel!!)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))

            return stub.deleteService(request)
        }
        abstractService.delete()

        return DeleteServiceResponse.newBuilder().setSuccess(true).build()
    }

    @RequiresPermission("services.sendCommand")
    override suspend fun sendCommand(request: SendCommandRequest): SendCommandResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = Node.instance.nodeServices.find { it.service.uuid == service.uuid }
        if (abstractService == null) {
            if (isLoggingRedirects()) {
                logger.warn(
                    "Service ${service.task.name}-${service.orderedId} is not registered on this node, trying to notify responsible node!"
                )
            }
            val correctNode =
                Node.instance.clusterProvider.remoteNodes.find { it.endpoint.name == service.node }
            if (correctNode?.endpoint?.name == ClusterHelper.getLocalNodeSnapshot().name) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Service ${service.task.name}-${service.orderedId} is not registered on this node, but assigned to this node! This might happen if the Node shuts down without removing the service, delete it manually in MongoDB (Shutting down the Cluster is recommended when doing this)!"
                    )
                }
            }

            if (correctNode == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to send command to Service ${service.task.name}-${service.orderedId} as it is not registered in this node and the responsible node was not found!"
                    )
                }
                return SendCommandResponse.newBuilder().setSuccess(false).build()
            }

            if (correctNode.channel == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to send command to Service ${service.task.name}-${service.orderedId} as the responsible node has no channel!"
                    )
                }
                return SendCommandResponse.newBuilder().setSuccess(false).build()
            }

            val stub =
                ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(correctNode.channel!!)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))

            return stub.sendCommand(request)
        }
        abstractService.command(request.command)

        return SendCommandResponse.newBuilder().setSuccess(true).build()
    }

    override suspend fun updatePlayerCount(
        request: UpdatePlayerCountRequest
    ): UpdatePlayerCountResponse {
        val service = Service.fromDefinition(request.service)

        val abstractService = Node.instance.nodeServices.find { it.service.uuid == service.uuid }
        if (abstractService == null) {
            if (isLoggingRedirects()) {
                logger.warn(
                    "Service ${service.task.name}-${service.orderedId} is not registered on this node, trying to notify responsible node!"
                )
            }
            val correctNode =
                Node.instance.clusterProvider.remoteNodes.find { it.endpoint.name == service.node }
            if (correctNode?.endpoint?.name == ClusterHelper.getLocalNodeSnapshot().name) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Service ${service.task.name}-${service.orderedId} is not registered on this node, but assigned to this node!"
                    )
                }
            }

            if (correctNode == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to update player count of Service ${service.task.name}-${service.orderedId} as it is not registered in this node and the responsible node was not found!"
                    )
                }
                return UpdatePlayerCountResponse.newBuilder().build()
            }

            if (correctNode.channel == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to update player count of Service ${service.task.name}-${service.orderedId} as the responsible node has no channel!"
                    )
                }
                return UpdatePlayerCountResponse.newBuilder().build()
            }

            val stub =
                ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(correctNode.channel!!)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))

            return stub.updatePlayerCount(request)
        }

        abstractService.service = abstractService.service.copy(playerCount = request.playerCount)

        return UpdatePlayerCountResponse.newBuilder().build()
    }

    @RequiresPermission("services.getLogs")
    override suspend fun getServiceLogs(request: GetServiceLogsRequest): GetServiceLogsResponse {
        val logs =
            ServiceLogHandler.getLogs(
                "${request.service.task.name}-${request.service.orderedId}",
                request.limit,
            )
        return getServiceLogsResponse { this.lines.addAll(logs) }
    }

    @RequiresPermission("services.streamLogs")
    override fun streamServiceLogs(
        request: StreamServiceLogsRequest
    ): Flow<StreamServiceLogsResponse> {
        return callbackFlow {
            val serviceName = "${request.service.task.name}-${request.service.orderedId}"
            val subscriptionJob =
                EventsService.subscribe<ServiceLogEvent> { event ->
                    val eventServiceName = "${event.service.task.name}-${event.service.orderedId}"
                    if (eventServiceName == serviceName) {
                        trySend(streamServiceLogsResponse { line = event.message })
                    }
                }

            awaitClose { subscriptionJob.cancel() }
        }
    }

    override suspend fun updateServiceSnapshot(
        request: UpdateServiceSnapshotRequest
    ): UpdateServiceSnapshotResponse {
        if (
            Node.instance.nodeServices.none { it.service.uuid.toString() == request.snapshot.uuid }
        ) {
            if (Node.instance.configProvider.config.nodeName == request.snapshot.node) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to update snapshot of service as it is not registered on this node!"
                    )
                }
                return UpdateServiceSnapshotResponse.newBuilder().build()
            }
            val correctNode =
                Node.instance.clusterProvider.remoteNodes.find {
                    it.endpoint.name == request.snapshot.node
                }
            if (correctNode == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to update snapshot of service as the responsible node was not found!"
                    )
                }
                return UpdateServiceSnapshotResponse.newBuilder().build()
            }

            if (isLoggingRedirects()) {
                logger.warn(
                    "Got request to update snapshot of service ${request.snapshot.uuid} but service is on Node ${request.snapshot.node}, redirecting!"
                )
            }

            val stub =
                ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(correctNode.channel!!)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))
            return stub.updateServiceSnapshot(request)
        }

        Node.instance.nodeServiceSnapshots.removeIf { it.uuid == request.snapshot.uuid }
        Node.instance.nodeServiceSnapshots.add(request.snapshot)

        updateSnapshotHooks.forEach { it(request.snapshot) }

        return UpdateServiceSnapshotResponse.newBuilder().setSnapshot(request.snapshot).build()
    }

    @RequiresPermission("services.getSnapshot")
    override suspend fun getLatestServiceSnapshot(
        request: GetLatestServiceSnapshotRequest
    ): GetLatestServiceSnapshotResponse {
        if (
            Node.instance.nodeServices.none { it.service.uuid.toString() == request.service.uuid }
        ) {
            if (Node.instance.configProvider.config.nodeName == request.service.node) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to get latest snapshot of service as it is not registered on this node!"
                    )
                }
                return GetLatestServiceSnapshotResponse.newBuilder().build()
            }
            val correctNode =
                Node.instance.clusterProvider.remoteNodes.find {
                    it.endpoint.name == request.service.node
                }
            if (correctNode == null) {
                if (isLoggingRedirects()) {
                    logger.error(
                        "Unable to get latest snapshot of service as the responsible node was not found!"
                    )
                }
                return GetLatestServiceSnapshotResponse.newBuilder().build()
            }
            if (isLoggingRedirects()) {
                logger.warn(
                    "Got request to get latest snapshot of service ${request.service.uuid} but service is on Node ${request.service.node}, redirecting!"
                )
            }
            val stub =
                ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(correctNode.channel!!)
                    .withInterceptors(AuthClientInterceptor(Node.instance.secret))
            return stub.getLatestServiceSnapshot(request)
        }
        return GetLatestServiceSnapshotResponse.newBuilder()
            .setSnapshot(
                Node.instance.nodeServiceSnapshots.find { it.uuid == request.service.uuid }
            )
            .build()
    }

    companion object {
        val updateSnapshotHooks: MutableSet<suspend (snapshot: ServiceSnapshot) -> Unit> =
            ConcurrentHashMap.newKeySet()
    }
}
