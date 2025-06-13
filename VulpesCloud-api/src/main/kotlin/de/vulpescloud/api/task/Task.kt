package de.vulpescloud.api.task

import de.vulpescloud.api.service.ServiceInfo
import de.vulpescloud.api.template.Template
import de.vulpescloud.api.version.SingleVersion

data class Task(
    val name: String,
    val nodes: List<String>,
    val templates: List<Template>,
    val maxMemory: Int,
    val maxPlayers: Int,
    val staticServices: Boolean,
    val minOnlineCount: Int,
    val serviceCount: Int,
    val services: List<ServiceInfo>,
    val maintenance: Boolean,
    val startPort: Int,
    val fallback: Boolean,
    val version: SingleVersion,
    val copyTemplateToStatic: Boolean,
    val serviceFactoryName: String,
    val environmentVars: MutableList<Pair<String, String>>
) {
    fun update(
        name: String = this.name,
        nodes: List<String> = this.nodes,
        templates: List<Template> = this.templates,
        maxMemory: Int = this.maxMemory,
        maxPlayers: Int = this.maxPlayers,
        staticServices: Boolean = this.staticServices,
        minOnlineCount: Int = this.minOnlineCount,
        serviceCount: Int = this.serviceCount,
        services: List<ServiceInfo> = this.services,
        maintenance: Boolean = this.maintenance,
        startPort: Int = this.startPort,
        fallback: Boolean = this.fallback,
        version: SingleVersion = this.version,
        copyTemplateToStatic: Boolean = this.copyTemplateToStatic,
    ): Task {
        return Task(
            name,
            nodes,
            templates,
            maxMemory,
            maxPlayers,
            staticServices,
            minOnlineCount,
            serviceCount,
            services,
            maintenance,
            startPort,
            fallback,
            version,
            copyTemplateToStatic,
            serviceFactoryName,
            environmentVars
        )
    }
}
