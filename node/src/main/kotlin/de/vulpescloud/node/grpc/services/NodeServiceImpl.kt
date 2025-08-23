package de.vulpescloud.node.grpc.services

import build.buf.gen.vulpescloud.node.v1.CreateServiceRequest
import build.buf.gen.vulpescloud.node.v1.CreateServiceResponse
import build.buf.gen.vulpescloud.node.v1.NodeServiceGrpcKt
import build.buf.gen.vulpescloud.node.v1.ServiceDefinition
import build.buf.gen.vulpescloud.node.v1.TaskDefinition

class NodeServiceImpl : NodeServiceGrpcKt.NodeServiceCoroutineImplBase() {
    override suspend fun createService(request: CreateServiceRequest): CreateServiceResponse {
        try {
            println("Received request to create service: ${request.task.name}")
            return CreateServiceResponse.newBuilder()
                .setService(
                    ServiceDefinition.newBuilder()
                        .setTask(
                            TaskDefinition.newBuilder()
                                .setName("test")
                                .build()
                        )
                        .build()
                )
                .build()
        } catch (e: Exception) {
            println("Error while creating service: ${e.message}")
            return CreateServiceResponse.newBuilder()
                .setService(
                    ServiceDefinition.newBuilder()
                        .setTask(
                            TaskDefinition.newBuilder()
                                .setName("test")
                                .build()
                        )
                        .build()
                )
                .build()
        }
    }
}