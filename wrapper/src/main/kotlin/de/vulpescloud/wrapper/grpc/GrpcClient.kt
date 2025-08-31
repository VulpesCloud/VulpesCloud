package de.vulpescloud.wrapper.grpc

import build.buf.gen.vulpescloud.services.v1.ServiceAPIServiceGrpcKt
import build.buf.gen.vulpescloud.tasks.v1.TasksAPIServiceGrpcKt
import io.grpc.ChannelCredentials
import io.grpc.Grpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.internal.ManagedChannelImplBuilder
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyChannelProvider
import java.io.File
import java.net.URI

class GrpcClient {

    private lateinit var channel: ManagedChannel
    lateinit var serviceAPI: ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub
    lateinit var tasksAPI: TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub

    fun connect(
        host: String = "127.0.0.1",
        port: Int = 6565,
        creds: ChannelCredentials,
        secret: String,
    ) {
        println("Connecting to $host:$port")

        println(URI(null, null, host, port, null, null, null).getAuthority())

        channel = NettyChannelBuilder
            .forAddress(host, port)
            .useTransportSecurity()
            .build()

        println("Channel authority: ${channel.authority()}")

        serviceAPI =
            ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        tasksAPI =
            TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
    }
}
