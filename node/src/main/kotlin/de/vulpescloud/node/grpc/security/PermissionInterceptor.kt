package de.vulpescloud.node.grpc.security

import de.vulpescloud.node.grpc.GrpcContextKeys
import de.vulpescloud.node.grpc.GrpcServiceRegistry
import de.vulpescloud.node.grpc.security.annotations.RequiresPermission
import io.grpc.*
import kotlinx.coroutines.runBlocking
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

class PermissionInterceptor : ServerInterceptor {

    private val publicRpcs =
        setOf("authenticate", "refreshToken", "isTokenValid")

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val methodName = call.methodDescriptor.fullMethodName
        val (serviceClass, rpcName) = methodName.split("/").let { it[0] to it[1] }

        if (rpcName.lowercase() in publicRpcs) {
            return next.startCall(call, headers)
        }

        val communicationType =
            headers.get(Metadata.Key.of("communication-type", Metadata.ASCII_STRING_MARSHALLER))

        if (communicationType != "internal") {
            val serviceInstance = GrpcServiceRegistry.get(serviceClass)
            if (serviceInstance != null) {
                val kFunction =
                    serviceInstance::class.declaredFunctions.find {
                        it.name.equals(rpcName, ignoreCase = true)
                    }
                val annotation = kFunction?.findAnnotation<RequiresPermission>()

                if (annotation != null) {
                    val username = GrpcContextKeys.USERNAME.get() ?: "unknown"
                    val required = annotation.permission

                    val hasAccess = runBlocking {
                        PermissionHelper.hasPermission(username, required)
                    }

                    if (!hasAccess) {
                        call.close(
                            Status.PERMISSION_DENIED.withDescription(
                                "User '$username' lacks permission: $required"
                            ),
                            Metadata(),
                        )
                        return object : ServerCall.Listener<ReqT>() {}
                    }
                } else {
                    call.close(
                        Status.UNAUTHENTICATED.withDescription(
                            "RPC $rpcName is not annotated with RequiresPermission"
                        ),
                        Metadata(),
                    )
                }
            } else {
                call.close(
                    Status.UNAUTHENTICATED.withDescription(
                        "Service $serviceClass is not registered"
                    ),
                    Metadata(),
                )
                return object : ServerCall.Listener<ReqT>() {}
            }
        }

        return next.startCall(call, headers)
    }
}
