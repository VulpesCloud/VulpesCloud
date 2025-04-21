package de.vulpescloud.api.task

import de.vulpescloud.api.service.Service
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
    val services: List<Service>,
    val maintenance: Boolean,
    val startPort: Int,
    val fallback: Boolean,
    val version: SingleVersion,
    val copyTemplateToStatic: Boolean
)
