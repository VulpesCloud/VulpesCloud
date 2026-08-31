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

package org.vulpesstudios.vulpescloud.node.grpc

import build.buf.gen.vulpescloud.auth.v1.AuthServiceGrpcKt
import build.buf.gen.vulpescloud.cluster.v2.ClusterAPIServiceGrpcKt
import build.buf.gen.vulpescloud.events.v1.EventServiceGrpcKt
import build.buf.gen.vulpescloud.players.v1.PlayerActionsServiceGrpcKt
import build.buf.gen.vulpescloud.players.v1.PlayersServiceGrpcKt
import build.buf.gen.vulpescloud.services.v1.ServiceAPIServiceGrpcKt
import build.buf.gen.vulpescloud.tasks.v1.TasksAPIServiceGrpcKt
import build.buf.gen.vulpescloud.templates.v1.TemplateServiceGrpcKt
import build.buf.gen.vulpescloud.virtualconfig.v1.VirtualConfigServiceGrpcKt
import io.grpc.Channel
import io.grpc.ClientInterceptors
import io.grpc.netty.NettyChannelBuilder
import io.netty.handler.ssl.SslContext
import org.vulpesstudios.vulpescloud.node.grpc.security.AuthClientInterceptor

class LocalGrpcClient {

    lateinit var channel: Channel
    lateinit var serviceAPI: ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub
    lateinit var tasksAPI: TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub
    lateinit var eventsAPI: EventServiceGrpcKt.EventServiceCoroutineStub
    lateinit var virtualConfigAPI: VirtualConfigServiceGrpcKt.VirtualConfigServiceCoroutineStub
    lateinit var clusterAPI: ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineStub
    lateinit var playerAPI: PlayersServiceGrpcKt.PlayersServiceCoroutineStub
    lateinit var authAPI: AuthServiceGrpcKt.AuthServiceCoroutineStub
    lateinit var playerActionsAPI: PlayerActionsServiceGrpcKt.PlayerActionsServiceCoroutineStub
    lateinit var templateAPI: TemplateServiceGrpcKt.TemplateServiceCoroutineStub

    fun connect(
        host: String = "127.0.0.1",
        port: Int = 6565,
        sslContext: SslContext? = null,
        secret: String,
    ) {
        val channelBuilder = NettyChannelBuilder.forAddress(host, port)
        if (sslContext != null) {
            channelBuilder.sslContext(sslContext)
        } else {
            channelBuilder.usePlaintext()
        }

        channel =
            ClientInterceptors.intercept(
                channelBuilder.build(),
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
        playerActionsAPI =
            PlayerActionsServiceGrpcKt.PlayerActionsServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        templateAPI = TemplateServiceGrpcKt.TemplateServiceCoroutineStub(channel)
        .withInterceptors(AuthClientInterceptor(secret))
    }
}
