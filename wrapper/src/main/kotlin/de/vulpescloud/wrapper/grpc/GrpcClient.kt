package de.vulpescloud.wrapper.grpc

import build.buf.gen.vulpescloud.auth.v1.AuthServiceGrpcKt
import build.buf.gen.vulpescloud.events.v1.EventServiceGrpcKt
import build.buf.gen.vulpescloud.node.v1.ClusterAPIServiceGrpcKt
import build.buf.gen.vulpescloud.players.v1.PlayersServiceGrpc
import build.buf.gen.vulpescloud.players.v1.PlayersServiceGrpcKt
import build.buf.gen.vulpescloud.services.v1.ServiceAPIServiceGrpc
import build.buf.gen.vulpescloud.services.v1.ServiceAPIServiceGrpcKt
import build.buf.gen.vulpescloud.tasks.v1.TasksAPIServiceGrpc
import build.buf.gen.vulpescloud.tasks.v1.TasksAPIServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.netty.NettyChannelBuilder
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import java.net.InetSocketAddress

class GrpcClient {

    lateinit var channel: ManagedChannel
    lateinit var serviceAPI: ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub
    lateinit var tasksAPI: TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub
    lateinit var playerAPI: PlayersServiceGrpcKt.PlayersServiceCoroutineStub
    lateinit var clusterAPI: ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineStub
    lateinit var authAPI: AuthServiceGrpcKt.AuthServiceCoroutineStub

    lateinit var futureServiceAPI: ServiceAPIServiceGrpc.ServiceAPIServiceFutureStub
    lateinit var futureTasksAPI: TasksAPIServiceGrpc.TasksAPIServiceFutureStub
    lateinit var futurePlayerAPI: PlayersServiceGrpc.PlayersServiceFutureStub

    lateinit var eventsAPI: EventServiceGrpcKt.EventServiceCoroutineStub

    fun connect(host: String = "127.0.0.1", port: Int = 6565, secret: String) {
        println("Connecting to $host:$port")

        //        val serverCertFile = File("vulpescloud/certs/server.crt")
        //
        //        val sslContext = GrpcSslContexts.forClient().trustManager(serverCertFile).build()

        channel =
            NettyChannelBuilder.forAddress(InetSocketAddress(host, port))
                .eventLoopGroup(NioEventLoopGroup()) // force NIO transport
                .channelType(NioSocketChannel::class.java) // force TCP
                // .sslContext(sslContext)
                .usePlaintext()
                .build()

        serviceAPI =
            ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        tasksAPI =
            TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        playerAPI =
            PlayersServiceGrpcKt.PlayersServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        futureServiceAPI =
            ServiceAPIServiceGrpc.newFutureStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        futureTasksAPI =
            TasksAPIServiceGrpc.newFutureStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        futurePlayerAPI =
            PlayersServiceGrpc.newFutureStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        eventsAPI =
            EventServiceGrpcKt.EventServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        clusterAPI =
            ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        authAPI =
            AuthServiceGrpcKt.AuthServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
    }
}
