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

package org.vulpesstudios.vulpescloud.api.tasks

import build.buf.gen.vulpescloud.tasks.v1.TaskDefinition
import kotlinx.serialization.Serializable
import org.vulpesstudios.vulpescloud.api.serversoftware.ServerSoftware
import org.vulpesstudios.vulpescloud.api.templates.Template

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
    val preferredNodes: List<String>,
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
                .addAllPreferredNodes(preferredNodes)
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
                taskDefinition.preferredNodesList,
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
