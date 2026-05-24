package de.vulpescloud.node.grpc

import build.buf.gen.vulpescloud.auth.v1.AuthServiceGrpcKt
import build.buf.gen.vulpescloud.events.v1.EventServiceGrpcKt
import build.buf.gen.vulpescloud.node.v1.ClusterAPIServiceGrpcKt
import build.buf.gen.vulpescloud.players.v1.PlayersServiceGrpcKt
import build.buf.gen.vulpescloud.services.v1.ServiceAPIServiceGrpcKt
import build.buf.gen.vulpescloud.tasks.v1.TasksAPIServiceGrpcKt
import build.buf.gen.vulpescloud.virtualconfig.v1.VirtualConfigServiceGrpcKt
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import io.grpc.Channel
import io.grpc.ClientInterceptors
import io.grpc.ManagedChannelBuilder

class LocalGrpcClient {

    lateinit var channel: Channel
    lateinit var serviceAPI: ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub
    lateinit var tasksAPI: TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub
    lateinit var eventsAPI: EventServiceGrpcKt.EventServiceCoroutineStub
    lateinit var virtualConfigAPI: VirtualConfigServiceGrpcKt.VirtualConfigServiceCoroutineStub
    lateinit var clusterAPI: ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineStub
    lateinit var playerAPI: PlayersServiceGrpcKt.PlayersServiceCoroutineStub
    lateinit var authAPI: AuthServiceGrpcKt.AuthServiceCoroutineStub

    fun connect(
        host: String = "127.0.0.1",
        port: Int = 6565,
        // creds: ChannelCredentials,
        secret: String,
    ) {
        channel =
            ClientInterceptors.intercept(
                ManagedChannelBuilder.forAddress(host, port).usePlaintext().build(),
                AuthClientInterceptor(secret),
            )

        serviceAPI =
            ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        tasksAPI =
            TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        eventsAPI =
            EventServiceGrpcKt.EventServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        virtualConfigAPI =
            VirtualConfigServiceGrpcKt.VirtualConfigServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        clusterAPI =
            ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        playerAPI =
            PlayersServiceGrpcKt.PlayersServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        authAPI =
            AuthServiceGrpcKt.AuthServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
    }
}
