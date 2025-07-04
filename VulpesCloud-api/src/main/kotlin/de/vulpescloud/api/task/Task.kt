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
)