package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.services.v1.DeleteServiceRequest
import build.buf.gen.vulpescloud.services.v1.GetAllServicesRequest
import build.buf.gen.vulpescloud.services.v1.ServiceAPIServiceGrpcKt
import build.buf.gen.vulpescloud.services.v1.StopServiceRequest
import de.vulpescloud.api.services.Service
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotation.Alias
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import io.grpc.Grpc
import kotlinx.coroutines.launch
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.processors.confirmation.annotation.Confirmation

@Alias(["ser"])
class ServiceCommand {

    @Command("service|ser list")
    suspend fun serList(source: CommandSource) {
        val channel =
            Grpc.newChannelBuilderForAddress(
                    Node.instance.configProvider.config.grpcHost,
                    Node.instance.configProvider.config.grpcPort,
                    Node.instance.creds,
                )
                .build()

        val stub =
            ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(Node.instance.secret))

        val response = stub.getAllServices(GetAllServicesRequest.newBuilder().build())

        val services = response.servicesList.map { Service.fromDefinition(it) }

        // TODO: WIP
        source.sendMessage("Services: $services")
    }

    @Confirmation
    @Command("service|ser stopAll")
    fun stopAllService(source: CommandSource) {
        NodeCoroutineScope.launch {
            source.sendMessage("Stopping all services...")
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(GetAllServicesRequest.newBuilder().build())
                .servicesList
                .forEach {
                    Node.instance.localGrpcClient.serviceAPI.stopService(
                        StopServiceRequest.newBuilder().setService(it).build()
                    )
                }
        }
    }

    @Confirmation
    @Command("service|ser deleteAll")
    fun deleteAllServices(source: CommandSource) {
        NodeCoroutineScope.launch {
            source.sendMessage("Deleting all services...")
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(GetAllServicesRequest.newBuilder().build())
                .servicesList
                .forEach {
                    Node.instance.localGrpcClient.serviceAPI.deleteService(
                        DeleteServiceRequest.newBuilder().setService(it).build()
                    )
                }
        }
    }
}
