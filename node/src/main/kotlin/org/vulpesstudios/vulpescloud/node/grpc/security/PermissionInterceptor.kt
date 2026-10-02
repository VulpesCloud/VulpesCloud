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
import com.google.protobuf.Message
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

                    val hasGlobalAccess = runBlocking {
                        PermissionHelper.hasPermission(username, required)
                    }

                    val listener = next.startCall(call, headers)
                    if (hasGlobalAccess) return listener

                    var rejected = false
                    var receivedMessage = false
                    return object : ServerCall.Listener<ReqT>() {
                        override fun onMessage(message: ReqT) {
                            receivedMessage = true
                            val authorized = runBlocking {
                                val resourceValues =
                                    extractResources(message, annotation.resources, serviceClass)
                                PermissionHelper.hasPermission(username, required, resourceValues)
                            }
                            if (!authorized) {
                                rejected = true
                                call.close(
                                    Status.PERMISSION_DENIED.withDescription(
                                        "User '$username' lacks permission: $required for the requested resource"
                                    ),
                                    Metadata(),
                                )
                                return
                            }
                            listener.onMessage(message)
                        }

                        override fun onHalfClose() {
                            if (!receivedMessage && !rejected) {
                                rejected = true
                                call.close(
                                    Status.PERMISSION_DENIED.withDescription(
                                        "User '$username' lacks permission: $required for the requested resource"
                                    ),
                                    Metadata(),
                                )
                            } else if (!rejected) listener.onHalfClose()
                        }
                        override fun onCancel() = listener.onCancel()
                        override fun onComplete() { if (!rejected && receivedMessage) listener.onComplete() }
                        override fun onReady() = listener.onReady()
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

    private suspend fun extractResources(
        request: Any?,
        mappings: Array<String>,
        serviceClass: String,
    ): Map<String, Set<String>> {
        val message = request as? Message ?: return emptyMap()
        val declared = mappings.mapNotNull { mapping ->
            val separator = mapping.indexOf('=')
            if (separator <= 0 || separator == mapping.lastIndex) return@mapNotNull null
            val scope = mapping.substring(0, separator).trim()
            val rawPath = mapping.substring(separator + 1).trim()
            val values = if (rawPath == "\$localNode") {
                setOf(org.vulpesstudios.vulpescloud.node.Node.instance.configProvider.config.nodeName)
            } else {
                val path = rawPath.split('.').filter(String::isNotBlank)
                resolvePath(message, path).toSet()
            }
            if (values.isEmpty()) null else scope to values
        }.associate { (scope, values) -> scope to values.toMutableSet() }.toMutableMap()
        inferResources(message, serviceClass).forEach { (scope, values) ->
            declared.getOrPut(scope) { mutableSetOf() }.addAll(values)
        }
        return expandServiceResources(declared.mapValues { it.value.toSet() }.toMutableMap())
    }

    private fun inferResources(message: Message, serviceClass: String): Map<String, Set<String>> {
        val values = mutableMapOf<String, MutableSet<String>>()
        val serviceName = serviceClass.substringAfterLast('.').lowercase()

        fun visit(current: Message, path: List<String>) {
            current.allFields.forEach { (field, rawValue) ->
                val fieldName = field.name.lowercase()
                val nestedValues = if (field.isRepeated) rawValue as Iterable<*> else listOf(rawValue)
                nestedValues.forEach { value ->
                    if (value is Message) {
                        visit(value, path + fieldName)
                    } else if (value is String && value.isNotBlank()) {
                        val scope = when {
                            fieldName == "nodename" -> "node"
                            fieldName == "taskname" -> "task"
                            fieldName in setOf("username", "user") -> "user"
                            fieldName == "groupname" -> "group"
                            fieldName in setOf("templatename", "template") -> "template"
                            fieldName in setOf("playername", "playeruuid") -> "player"
                            fieldName == "node" && path.any { it in setOf("service", "snapshot") } -> "node"
                            fieldName in setOf("rolloutid", "rollout_id") -> "rollout"
                            fieldName == "targetserver" -> "service"
                            fieldName == "uuid" && path.any { it == "service" } -> "service"
                            fieldName in setOf("templateid", "templatename") -> "template"
                            fieldName in setOf("virtualconfigname", "configname") -> "virtualconfig"
                            fieldName == "uuid" && serviceName.contains("serviceapi") -> "service"
                            fieldName == "uuid" && serviceName.contains("playeraction") -> "player"
                            fieldName == "name" && path.lastOrNull() == "task" -> "task"
                            fieldName == "name" && path.any { it.contains("template") } -> "template"
                            fieldName == "name" && serviceName.contains("template") -> "template"
                            fieldName == "name" && path.any { it in setOf("config", "virtualconfig") } -> "virtualconfig"
                            fieldName == "name" && path.lastOrNull() in setOf("user", "group", "template", "player") -> path.last()
                            fieldName == "name" && path.isEmpty() && serviceName.contains("cluster") -> "node"
                            fieldName == "name" && path.isEmpty() && serviceName.contains("task") -> "task"
                            fieldName == "name" && path.isEmpty() && serviceName.contains("auth") -> "user"
                            fieldName == "name" && path.isEmpty() && serviceName.contains("template") -> "template"
                            fieldName == "name" && path.isEmpty() && serviceName.contains("virtualconfig") -> "virtualconfig"
                            fieldName == "name" && path.isEmpty() && serviceName.contains("serviceapi") -> "service"
                            else -> null
                        }
                        if (scope != null) values.getOrPut(scope) { mutableSetOf() }.add(value)
                    }
                }
            }
        }

        visit(message, emptyList())
        return values
    }

    private suspend fun expandServiceResources(resources: MutableMap<String, Set<String>>): Map<String, Set<String>> {
        val requested = resources["service"].orEmpty()
        if (requested.isEmpty()) return resources
        val services = runCatching {
            org.vulpesstudios.vulpescloud.node.Node.instance.localGrpcClient.serviceAPI
                .getAllServices(build.buf.gen.vulpescloud.services.v1.getAllServicesRequest {})
                .servicesList
        }.getOrDefault(emptyList())
        val matched = services.filter { service ->
            requested.any { it == service.uuid || it.equals("${service.task.name}-${service.orderedId}", true) }
        }
        if (matched.isEmpty()) return resources
        val expanded = resources.toMutableMap()
        expanded["service"] = matched.map { "${it.task.name}-${it.orderedId}" }.toSet()
        expanded.getOrPut("task") { emptySet() }.let { expanded["task"] = it + matched.map { service -> service.task.name } }
        expanded.getOrPut("node") { emptySet() }.let { expanded["node"] = it + matched.map { service -> service.node } }
        return expanded
    }

    private fun resolvePath(value: Any?, path: List<String>): List<String> {
        if (value == null || path.isEmpty()) return emptyList()
        if (value is Iterable<*>) return value.flatMap { resolvePath(it, path) }
        val message = value as? Message ?: return emptyList()
        val field = message.descriptorForType.findFieldByName(path.first()) ?: return emptyList()
        val fieldValue = message.getField(field)
        if (path.size == 1) {
            return when (fieldValue) {
                is String -> if (fieldValue.isBlank()) emptyList() else listOf(fieldValue)
                is Iterable<*> -> fieldValue.filterIsInstance<String>().filter(String::isNotBlank)
                else -> emptyList()
            }
        }
        return resolvePath(fieldValue, path.drop(1))
    }
}
