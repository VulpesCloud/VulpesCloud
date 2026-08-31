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

package org.vulpesstudios.vulpescloud.node.setup.setups

import build.buf.gen.vulpescloud.tasks.v1.createTaskRequest
import build.buf.gen.vulpescloud.tasks.v1.getAllTasksRequest
import build.buf.gen.vulpescloud.templates.v1.createTemplateRequest
import build.buf.gen.vulpescloud.templates.v1.templateDefinition
import build.buf.gen.vulpescloud.templates.v1.templateLocation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.vulpesstudios.vulpescloud.api.serversoftware.ServerSoftware
import org.vulpesstudios.vulpescloud.api.serversoftware.SoftwareType
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.api.templates.Template
import org.vulpesstudios.vulpescloud.api.templates.TemplateLocation
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.NodeCoroutineScope
import org.vulpesstudios.vulpescloud.node.serversoftware.ServerSoftwareDownloader
import org.vulpesstudios.vulpescloud.node.setup.Setup
import org.vulpesstudios.vulpescloud.node.setup.annotations.SetupFinish
import org.vulpesstudios.vulpescloud.node.setup.annotations.SetupQuestion
import org.vulpesstudios.vulpescloud.node.setup.answers.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class TaskSetup() : Setup {

    override val header: String = "Task Setup"
    private var task: Task =
        Task(
            "UNKNOWN",
            1,
            1,
            26655,
            listOf(),
            false,
            0,
            0,
            maintenance = false,
            copyTemplatesToStatic = false,
            serviceFactoryName = "local",
            preferredNode = "",
            maxPlayers = 1,
            software = ServerSoftware("UNKNOWN", "UNKNOWN", 0, "", "", SoftwareType.SERVER),
            attributes = emptyMap(),
            jvmArgs = emptyList(),
            envVars = emptyList(),
            fallback = false,
            autoStart = false,
        )

    companion object {
        var downloader: ServerSoftwareDownloader? = null
            private set
    }

    @SetupQuestion(0, "What should be the name of the task?")
    fun q0(name: String): Boolean {
        if (name.contains(" ")) return false

        task = task.copy(name = name)
        return true
    }

    @SetupQuestion(
        1,
        "What software should be running on this task?",
        SoftwareNameSetupAnswer::class,
        true,
    )
    fun q1(software: String): Boolean {
        task = task.copy(software = task.software.copy(name = software))

        if (software == "Minestom") {
            Node.instance.terminal.printSetup("Note: You will need to add your own server jar!")
            Node.instance.setupProvider.currentQuestionIndex = 2
            task = task.copy(software = task.software.copy(version = "CUSTOM"))
            return true
        }

        downloader = Node.instance.serverSoftwareProvider.getFromDisplayName(software)

        if (downloader == null) {
            Node.instance.terminal.printSetup(
                "Could not find downloader for $software! How? Just how was this even possible?!"
            )
            return false
        }

        return true
    }

    @SetupQuestion(
        2,
        "What version of the software should be running on this task?",
        SoftwareVersionSetupAnswer::class,
        true,
    )
    fun q2(version: String): Boolean {
        return CompletableFuture.supplyAsync {
                runBlocking {
                    val ver = downloader?.getLatestVersion(version)

                    if (ver == null) {
                        Node.instance.terminal.printSetup("The version $version is not found!")
                        return@runBlocking false
                    }

                    task =
                        task.copy(
                            software =
                                task.software.copy(
                                    version = ver.version,
                                    build = ver.build,
                                    url = ver.url,
                                    pluginDir = ver.pluginDir,
                                    type = ver.type,
                                )
                        )
                    true
                }
            }
            .get(5, TimeUnit.SECONDS)
    }

    @SetupQuestion(
        3,
        "How much memory should services of this task be able to use? (Value must be in MB)",
        MemorySetupAnswer::class,
    )
    fun q3(answer: String): Boolean {
        task = task.copy(maxMemory = answer.toLong())
        return true
    }

    @SetupQuestion(
        4,
        "At what port should services from this task start?",
        NullSetupAnswer::class,
        false,
        ["25565", "26655"],
    )
    fun q4(answer: String): Boolean {
        task = task.copy(startPort = answer.toLong())
        return true
    }

    @SetupQuestion(5, "Should services on this task be static?", BooleanSetupAnswer::class, true)
    fun q5(answer: String): Boolean {
        task = task.copy(staticServices = answer.toBoolean())
        return true
    }

    @SetupQuestion(
        6,
        "How many service should be always started on this task?",
        NullSetupAnswer::class,
        false,
        ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"],
    )
    fun q6(answer: String): Boolean {
        task = task.copy(minOnlineServices = answer.toInt())
        return true
    }

    @SetupQuestion(
        7,
        "Should this task be in maintenance mode? (This prevents auto starting of services)",
        BooleanSetupAnswer::class,
        true,
    )
    fun q7(answer: String): Boolean {
        task = task.copy(maintenance = answer.toBoolean())
        return true
    }

    @SetupQuestion(8, "How many players should be allowed to join this task?")
    fun q8(answer: String): Boolean {
        task = task.copy(maxPlayers = answer.toInt())
        return true
    }

    @SetupFinish
    fun finish() {
        NodeCoroutineScope.launch {
            val tasks =
                Node.instance.localGrpcClient.tasksAPI
                    .getAllTasks(getAllTasksRequest {})
                    .tasksList
                    .map { Task.fromDefinition(it) }

            if (tasks.none { it.software.type == SoftwareType.SERVER })
                task = task.copy(fallback = true)

            task =
                task.copy(
                    preferredNode = Node.instance.configProvider.config.nodeName,
                    templates =
                        listOf(
                            Template(
                                task.name,
                                0,
                                task.name,
                                TemplateLocation(
                                    "LOCAL",
                                    Node.instance.configProvider.config.nodeName,
                                ),
                            )
                        ),
                )

            Node.instance.localGrpcClient.templateAPI.createTemplate(
                createTemplateRequest {
                    template = templateDefinition {
                        name = task.name
                        weight = 0
                        id = task.name
                        location = templateLocation {
                            storageName = "LOCAL"
                            nodeName = Node.instance.configProvider.config.nodeName
                        }
                        version = ""
                        enabled = true
                    }
                    destination = templateLocation {
                        storageName = "LOCAL"
                        nodeName = Node.instance.configProvider.config.nodeName
                    }
                }
            )

            Node.instance.localGrpcClient.tasksAPI.createTask(
                createTaskRequest { this.task = this@TaskSetup.task.toDefinition() }
            )
        }
    }
}
