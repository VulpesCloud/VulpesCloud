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

package org.vulpesstudios.vulpescloud.node.grpc.security

import org.vulpesstudios.vulpescloud.node.grpc.GrpcContextKeys
import org.vulpesstudios.vulpescloud.node.grpc.GrpcServiceRegistry
import org.vulpesstudios.vulpescloud.node.grpc.security.annotations.RequiresPermission
import io.grpc.*
import kotlinx.coroutines.runBlocking
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

class PermissionInterceptor : ServerInterceptor {

    private val publicRpcs =
        setOf("authenticate", "refreshtoken", "istokenvalid")

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
