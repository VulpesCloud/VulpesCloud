package de.vulpescloud.node.grpc

import io.grpc.Context

object GrpcContextKeys {
    val USERNAME: Context.Key<String> = Context.key("username")
    val ROLE: Context.Key<String> = Context.key("role")
    val PERMISSIONS: Context.Key<List<String>> = Context.key("permissions")
    val COMMUNICATION_TYPE: Context.Key<String> = Context.key("communicationType")
}
