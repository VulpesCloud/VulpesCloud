package de.vulpescloud.node.grpc

import build.buf.gen.vulpescloud.services.v1.ServiceAPIServiceGrpcKt
import build.buf.gen.vulpescloud.tasks.v1.TasksAPIServiceGrpcKt
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import io.grpc.ChannelCredentials
import io.grpc.Grpc
import io.grpc.ManagedChannel

class LocalGrpcClient {

    private lateinit var channel: ManagedChannel
    lateinit var serviceAPI: ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub
    lateinit var tasksAPI: TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub

    fun connect(
        host: String = "127.0.0.1",
        port: Int = 6565,
        creds: ChannelCredentials,
        secret: String,
    ) {
        channel =
            Grpc.newChannelBuilderForAddress(host, port, creds)
                .intercept(AuthClientInterceptor(secret))
                .build()

        serviceAPI = ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(channel)
        tasksAPI = TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub(channel)
    }
}
