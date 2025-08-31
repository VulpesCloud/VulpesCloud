package de.vulpescloud.wrapper

import build.buf.gen.vulpescloud.services.v1.GetAllServicesRequest
import de.vulpescloud.wrapper.grpc.GrpcClient
import io.grpc.TlsChannelCredentials
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

@OptIn(DelicateCoroutinesApi::class)
class Wrapper(args: Array<String>) {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Wrapper(args)
        }

        lateinit var instance: Wrapper
    }

    val grpcClient = GrpcClient()

    init {
        instance = this

        System.setProperty("io.grpc.internal.DnsNameResolverProvider.enable_jndi", "false")
        System.setProperty("io.grpc.internal.ManagedChannelImplBuilder.forceUseTarget", "true")
        System.setProperty("io.grpc.internal.ManagedChannelProvider.disableUnixSocketResolver", "true")


        val serverCertBytes = File("vulpescloud/certs/server.crt")

        grpcClient.connect(
            System.getenv("grpc_hostname"),
            System.getenv("grpc_port").toInt(),
            TlsChannelCredentials.newBuilder().trustManager(serverCertBytes).build(),
            System.getenv("secret"),
        )

        GlobalScope.launch {
            grpcClient.serviceAPI.getAllServices(GetAllServicesRequest.getDefaultInstance())
        }
    }
}
