package de.vulpescloud.wrapper.grpc

import build.buf.gen.vulpescloud.services.v1.ServiceAPIServiceGrpcKt
import build.buf.gen.vulpescloud.tasks.v1.TasksAPIServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import java.io.File

class GrpcClient {

    private lateinit var channel: ManagedChannel
    lateinit var serviceAPI: ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub
    lateinit var tasksAPI: TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub

    fun connect(host: String = "127.0.0.1", port: Int = 6565, secret: String) {
        println("Connecting to $host:$port")

        val serverCertFile = File("vulpescloud/certs/server.crt")

        val sslContext = GrpcSslContexts.forClient().trustManager(serverCertFile).build()

        channel =
            NettyChannelBuilder.forAddress(host, port)
                .eventLoopGroup(NioEventLoopGroup()) // force NIO transport
                .channelType(NioSocketChannel::class.java) // force TCP
                .sslContext(sslContext)
                .build()

        serviceAPI =
            ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        tasksAPI =
            TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
    }
}
