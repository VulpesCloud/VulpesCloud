package de.vulpescloud.node.grpc

object GrpcServiceRegistry {
    private val services = mutableMapOf<String, Any>()

    fun register(name: String, instance: Any) {
        services[name] = instance
    }

    fun get(name: String): Any? = services[name]
}
