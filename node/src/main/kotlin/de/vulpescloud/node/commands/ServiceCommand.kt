package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.services.v1.GetAllServicesRequest
import build.buf.gen.vulpescloud.services.v1.ServiceAPIServiceGrpcKt
import de.vulpescloud.api.services.Service
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotation.Alias
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import io.grpc.Grpc
import org.incendo.cloud.annotations.Command

@Alias(["ser"])
class ServiceCommand {

    @Command("service|ser list")
    suspend fun serList(source: CommandSource) {
        val channel = Grpc.newChannelBuilderForAddress(
            Node.instance.configProvider.config.grpcHost,
            Node.instance.configProvider.config.grpcPort,
            Node.instance.creds,
        ).build()

        val stub = ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(channel)
            .withInterceptors(AuthClientInterceptor(Node.instance.secret))

        val response = stub.getAllServices(
            GetAllServicesRequest.newBuilder().build()
        )

        val services = response.servicesList.map { Service.fromDefinition(it) }

        // TODO: WIP
        source.sendMessage("Services: $services")
    }

}