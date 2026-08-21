package de.vulpescloud.api.tasks

import build.buf.gen.vulpescloud.tasks.v1.TaskDefinition
import de.vulpescloud.api.serversoftware.ServerSoftware
import de.vulpescloud.api.templates.Template
import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val name: String,
    val maxMemory: Long,
    val minMemory: Long,
    val startPort: Long,
    val templates: List<Template>,
    val staticServices: Boolean,
    val minOnlineServices: Int,
    val maxOnlineServices: Int,
    val maintenance: Boolean,
    val copyTemplatesToStatic: Boolean,
    val serviceFactoryName: String,
    val preferredNode: String,
    val maxPlayers: Int,
    val software: ServerSoftware,
    val attributes: Map<String, String>,
    val jvmArgs: List<String>,
    val envVars: List<String>,
    val fallback: Boolean,
    val autoStart: Boolean,
) {

    fun toDefinition(): TaskDefinition {
        val builder =
            TaskDefinition.newBuilder()
                .setName(name)
                .setMaximumMemory(maxMemory)
                .setMinimumMemory(minMemory)
                .setStartPort(startPort)
                .setStaticServices(staticServices)
                .setMinOnlineServices(minOnlineServices)
                .setMaxOnlineServices(maxOnlineServices)
                .setMaintenance(maintenance)
                .setCopyTemplateToStatic(copyTemplatesToStatic)
                .setServiceFactoryName(serviceFactoryName)
                .setPreferredNode(preferredNode)
                .setMaxPlayers(maxPlayers)
                .setServerSoftware(software.toDefinition())
                .setFallback(fallback)
                .putAllAttributes(attributes)
                .addAllJvmArgs(jvmArgs)
                .addAllEnvVars(envVars)
                .setAutoStart(autoStart)

        templates.forEach { builder.addTemplates(it.toDefinition()) }
        return builder.build()
    }

    companion object {
        fun fromDefinition(taskDefinition: TaskDefinition): Task {
            return Task(
                taskDefinition.name,
                taskDefinition.maximumMemory,
                taskDefinition.minimumMemory,
                taskDefinition.startPort,
                taskDefinition.templatesList.map { Template.fromDefinition(it) },
                taskDefinition.staticServices,
                taskDefinition.minOnlineServices,
                taskDefinition.maxOnlineServices,
                taskDefinition.maintenance,
                taskDefinition.copyTemplateToStatic,
                taskDefinition.serviceFactoryName,
                taskDefinition.preferredNode,
                taskDefinition.maxPlayers,
                ServerSoftware.fromDefinition(taskDefinition.serverSoftware),
                taskDefinition.attributesMap,
                taskDefinition.jvmArgsList,
                taskDefinition.envVarsList,
                taskDefinition.fallback,
                taskDefinition.autoStart,
            )
        }
    }
}
