package de.vulpescloud.node.setup.setups

import build.buf.gen.vulpescloud.tasks.v1.createTaskRequest
import build.buf.gen.vulpescloud.tasks.v1.getAllTasksRequest
import de.vulpescloud.api.serversoftware.ServerSoftware
import de.vulpescloud.api.serversoftware.SoftwareType
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.api.templates.Template
import de.vulpescloud.api.templates.TemplateStorages
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.serversoftware.impl.*
import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import de.vulpescloud.node.setup.answers.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
            false,
            false,
            "local",
            "",
            1,
            ServerSoftware("UNKNOWN", "UNKNOWN", 0, "", "", SoftwareType.SERVER),
            null,
            emptyList(),
            emptyList(),
            false,
        )

    companion object {
        var softwareName: String? = null
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

        softwareName = software

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
                    val ver =
                        when (softwareName) {
                            "Velocity" -> VelocityDownloader.getLatestVersion(version)
                            "Paper" -> PaperDownloader.getLatestVersion(version)
                            "Purpur" -> PurpurDownloader.getLatestVersion(version)
                            "Folia" -> FoliaDownloader.getLatestVersion(version)
                            "Canvas" -> CanvasDownloader.getLatestVersion(version)
                            else -> null
                        }

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
        "How much memory should this service be able to use?",
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

    @SetupQuestion(7, "Should this task be in maintenance mode?", BooleanSetupAnswer::class, true)
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
                    templates = listOf(Template(task.name, TemplateStorages.LOCAL, 0)),
                )

            Node.instance.localGrpcClient.tasksAPI.createTask(
                createTaskRequest { this.task = this@TaskSetup.task.toDefinition() }
            )
        }
    }
}
