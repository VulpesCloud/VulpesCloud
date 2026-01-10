package de.vulpescloud.api.tasks

import build.buf.gen.vulpescloud.tasks.v1.TaskDefinition
import de.vulpescloud.api.serversoftware.ServerSoftware
import de.vulpescloud.api.templates.Template
import kotlinx.serialization.Serializable
import org.bson.*

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
    val attributes: Map<String, String> = emptyMap(),
    val jvmArgs: List<String> = emptyList(),
    val envVars: List<String> = emptyList(),
    val fallback: Boolean,
) {

  fun toDocument(): BsonDocument =
      BsonDocument().apply {
        append("name", BsonString(name))
        append("maxMemory", BsonInt64(maxMemory))
        append("minMemory", BsonInt64(minMemory))
        append("startPort", BsonInt64(startPort))
        append("templates", BsonArray(templates.map { it.toDocument() }))
        append("staticServices", BsonBoolean(staticServices))
        append("minOnlineServices", BsonInt32(minOnlineServices))
        append("maxOnlineServices", BsonInt32(maxOnlineServices))
        append("maintenance", BsonBoolean(maintenance))
        append("copyTemplatesToStatic", BsonBoolean(copyTemplatesToStatic))
        append("serviceFactoryName", BsonString(serviceFactoryName))
        append("preferredNode", BsonString(preferredNode))
        append("maxPlayers", BsonInt32(maxPlayers))
        append("software", software.toDocument())
        append(
            "attributes",
            BsonDocument().apply {
              attributes.forEach { (key, value) -> append(key, BsonString(value)) }
            },
        )
        append("jvmArgs", BsonArray(jvmArgs.map { BsonString(it) }))
        append("envVars", BsonArray(envVars.map { BsonString(it) }))
        append("fallback", BsonBoolean(fallback))
      }

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
          taskDefinition.attributes,
          taskDefinition.jvmArgsList,
          taskDefinition.envVarsList,
          taskDefinition.fallback,
      )
    }

    fun fromDocument(document: BsonDocument): Task =
        Task(
            document.getString("name").value,
            document.getInt64("maxMemory").value,
            document.getInt64("minMemory").value,
            document.getInt64("startPort").value,
            document.getArray("templates").map { Template.fromDocument(it.asDocument()) },
            document.getBoolean("staticServices").value,
            document.getInt32("minOnlineServices").value,
            document.getInt32("maxOnlineServices").value,
            document.getBoolean("maintenance").value,
            document.getBoolean("copyTemplatesToStatic").value,
            document.getString("serviceFactoryName").value,
            document.getString("preferredNode").value,
            document.getInt32("maxPlayers").value,
            ServerSoftware.fromDocument(document.getDocument("software")),
            document.getDocument("attributes")?.entries?.associate {
              it.key to it.value.asString().value
            } ?: emptyMap(),
            document.getArray("jvmArgs").map { it.asString().value },
            document.getArray("envVars").map { it.asString().value },
            document.getBoolean("fallback").value,
        )
  }
}
