package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.services.v1.PrepareServiceByTaskRequest
import build.buf.gen.vulpescloud.services.v1.StartServiceRequest
import build.buf.gen.vulpescloud.tasks.v1.CreateTaskRequest
import de.vulpescloud.api.serversoftware.ServerSoftware
import de.vulpescloud.api.serversoftware.SoftwareType
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.services.impl.docker.DockerService
import kotlinx.coroutines.launch
import org.incendo.cloud.annotations.Command

class DebugCommand {

    @Command("debug create task default")
    fun createDefaultTask(source: CommandSource) {
        NodeCoroutineScope.launch {
            source.sendMessage("Creating default task")
            Node.instance.localGrpcClient.tasksAPI.createTask(
                CreateTaskRequest.newBuilder()
                    .setTask(
                        Task(
                                "Lobby",
                                2048,
                                512,
                                25565,
                                listOf(),
                                true,
                                1,
                                1,
                                false,
                                false,
                                "local",
                                "",
                                1,
                                ServerSoftware(
                                    "Purpur",
                                    "1.21.8",
                                    2493,
                                    "https://api.purpurmc.org/v2/purpur/1.21.8/2493/download",
                                    "plugins",
                                    SoftwareType.SERVER,
                                ),
                            )
                            .toDefinition()
                    )
                    .build()
            )
            source.sendMessage("Successfully created default task")
        }
    }

    @Command("debug create task defaultproxy")
    fun createDefaultTaskProxy(source: CommandSource) {
        NodeCoroutineScope.launch {
            source.sendMessage("Creating default task proxy")
            Node.instance.localGrpcClient.tasksAPI.createTask(
                CreateTaskRequest.newBuilder()
                    .setTask(
                        Task(
                                "Proxy",
                                512,
                                512,
                                28879,
                                listOf(),
                                true,
                                1,
                                1,
                                false,
                                false,
                                "docker",
                                "",
                                1,
                                ServerSoftware(
                                    "Velocity",
                                    "3.4.0-SNAPSHOT",
                                    533,
                                    "https://fill-data.papermc.io/v1/objects/cb33a12f4b6057fe2f862212ab4c033202b86172527168a41e30a30c1d05d27e/velocity-3.4.0-SNAPSHOT-533.jar",
                                    "plugins",
                                    SoftwareType.PROXY,
                                ),
                            )
                            .toDefinition()
                    )
                    .build()
            )
            source.sendMessage("Successfully created default proxy task")
        }
    }

    @Command("debug start service default")
    fun startServiceOnDefaultTask(source: CommandSource) {
        NodeCoroutineScope.launch {
            source.sendMessage("Starting service on default task")
            Node.instance.localGrpcClient.serviceAPI
                .prepareServiceByTask(
                    PrepareServiceByTaskRequest.newBuilder()
                        .setTask(
                            Task(
                                    "Lobby",
                                    2048,
                                    512,
                                    25565,
                                    listOf(),
                                    true,
                                    1,
                                    1,
                                    false,
                                    false,
                                    "local",
                                    "",
                                    1,
                                    ServerSoftware(
                                        "Purpur",
                                        "1.21.8",
                                        2493,
                                        "https://api.purpurmc.org/v2/purpur/1.21.8/2493/download",
                                        "plugins",
                                        SoftwareType.SERVER,
                                    ),
                                )
                                .toDefinition()
                        )
                        .build()
                )
                .let {
                    Node.instance.localGrpcClient.serviceAPI.startService(
                        StartServiceRequest.newBuilder().setService(it.service).build()
                    )
                }
            source.sendMessage("Successfully started service on default task")
        }
    }

    @Command("debug start service defaultproxy")
    fun startServiceOnDefaultTaskProxy(source: CommandSource) {
        NodeCoroutineScope.launch {
            source.sendMessage("Starting service on default proxy task")
            Node.instance.localGrpcClient.serviceAPI
                .prepareServiceByTask(
                    PrepareServiceByTaskRequest.newBuilder()
                        .setTask(
                            Task(
                                    "Proxy",
                                    512,
                                    512,
                                    28879,
                                    listOf(),
                                    true,
                                    1,
                                    1,
                                    false,
                                    false,
                                    "local",
                                    "",
                                    1,
                                    ServerSoftware(
                                        "Velocity",
                                        "3.4.0-SNAPSHOT",
                                        533,
                                        "https://fill-data.papermc.io/v1/objects/cb33a12f4b6057fe2f862212ab4c033202b86172527168a41e30a30c1d05d27e/velocity-3.4.0-SNAPSHOT-533.jar",
                                        "plugins",
                                        SoftwareType.PROXY,
                                    ),
                                )
                                .toDefinition()
                        )
                        .build()
                )
                .let {
                    Node.instance.localGrpcClient.serviceAPI.startService(
                        StartServiceRequest.newBuilder().setService(it.service).build()
                    )
                }
            source.sendMessage("Successfully started service on default proxy task")
        }
    }

    @Command("debug cmd")
    fun cmdCmd(source: CommandSource) {
        NodeCoroutineScope.launch {
            source.sendMessage("making command exec on proxy 1")
            DockerService(
                    Node.instance.nodeServices
                        .filterIsInstance<DockerService>()
                        .first { it.service.task.software.type == SoftwareType.PROXY }
                        .service
                )
                .command("velocity plugins")
        }
    }
}
