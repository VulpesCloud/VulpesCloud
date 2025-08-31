package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.services.v1.CreateServiceRequest
import build.buf.gen.vulpescloud.services.v1.StartServiceRequest
import build.buf.gen.vulpescloud.tasks.v1.CreateTaskRequest
import de.vulpescloud.api.serversoftware.ServerSoftware
import de.vulpescloud.api.serversoftware.SoftwareType
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
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

    @Command("debug start service default")
    fun startServiceOnDefaultTask(source: CommandSource) {
        NodeCoroutineScope.launch {
            source.sendMessage("Starting service on default task")
            Node.instance.localGrpcClient.serviceAPI
                .createService(
                    CreateServiceRequest.newBuilder()
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
}
