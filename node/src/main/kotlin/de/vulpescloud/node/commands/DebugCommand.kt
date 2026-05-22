package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.tasks.v1.getByNameRequest
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.virtualconfig.VirtualConfigDebugHelper
import kotlinx.coroutines.launch
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command

class DebugCommand {

    //    private val debugProxyTask =
    //        Task(
    //            "Proxy",
    //            512,
    //            512,
    //            28879,
    //            listOf(),
    //            true,
    //            1,
    //            1,
    //            false,
    //            false,
    //            "local",
    //            "",
    //            1,
    //            ServerSoftware(
    //                "Velocity",
    //                "3.4.0-SNAPSHOT",
    //                533,
    //
    // "https://fill-data.papermc.io/v1/objects/cb33a12f4b6057fe2f862212ab4c033202b86172527168a41e30a30c1d05d27e/velocity-3.4.0-SNAPSHOT-533.jar",
    //                "plugins",
    //                SoftwareType.PROXY,
    //            ),
    //            emptyMap(),
    //            emptyList(),
    //            emptyList(),
    //            false,
    //        )
    //
    //    @Command("debug create task default")
    //    fun createDefaultTask(source: CommandSource) {
    //        NodeCoroutineScope.launch {
    //            source.sendMessage("Creating default task")
    //            Node.instance.localGrpcClient.tasksAPI.createTask(
    //                CreateTaskRequest.newBuilder()
    //                    .setTask(
    //                        Task(
    //                                "Lobby",
    //                                2048,
    //                                512,
    //                                25565,
    //                                listOf(),
    //                                true,
    //                                1,
    //                                1,
    //                                false,
    //                                false,
    //                                "local",
    //                                "",
    //                                1,
    //                                ServerSoftware(
    //                                    "Purpur",
    //                                    "1.21.8",
    //                                    2493,
    //                                    "https://api.purpurmc.org/v2/purpur/1.21.8/2493/download",
    //                                    "plugins",
    //                                    SoftwareType.SERVER,
    //                                ),
    //                                emptyMap(),
    //                                emptyList(),
    //                                emptyList(),
    //                                true,
    //                            )
    //                            .toDefinition()
    //                    )
    //                    .build()
    //            )
    //            source.sendMessage("Successfully created default task")
    //        }
    //    }
    //
    //    @Command("debug create task defaultproxy")
    //    fun createDefaultTaskProxy(source: CommandSource) {
    //        NodeCoroutineScope.launch {
    //            source.sendMessage("Creating default task proxy")
    //            Node.instance.localGrpcClient.tasksAPI.createTask(
    //                CreateTaskRequest.newBuilder()
    //                    .setTask(
    //                        Task(
    //                                "Proxy",
    //                                512,
    //                                512,
    //                                28879,
    //                                listOf(),
    //                                true,
    //                                1,
    //                                1,
    //                                false,
    //                                false,
    //                                "docker",
    //                                "",
    //                                1,
    //                                ServerSoftware(
    //                                    "Velocity",
    //                                    "3.4.0-SNAPSHOT",
    //                                    533,
    //
    // "https://fill-data.papermc.io/v1/objects/cb33a12f4b6057fe2f862212ab4c033202b86172527168a41e30a30c1d05d27e/velocity-3.4.0-SNAPSHOT-533.jar",
    //                                    "plugins",
    //                                    SoftwareType.PROXY,
    //                                ),
    //                                emptyMap(),
    //                                emptyList(),
    //                                emptyList(),
    //                                false,
    //                            )
    //                            .toDefinition()
    //                    )
    //                    .build()
    //            )
    //            source.sendMessage("Successfully created default proxy task")
    //        }
    //    }
    //
    //    @Command("debug start service default")
    //    fun startServiceOnDefaultTask(source: CommandSource) {
    //        NodeCoroutineScope.launch {
    //            source.sendMessage("Starting service on default task")
    //            Node.instance.localGrpcClient.serviceAPI
    //                .prepareServiceByTask(
    //                    PrepareServiceByTaskRequest.newBuilder()
    //                        .setTask(
    //                            Task(
    //                                    "Lobby",
    //                                    2048,
    //                                    512,
    //                                    25565,
    //                                    listOf(),
    //                                    true,
    //                                    1,
    //                                    1,
    //                                    false,
    //                                    false,
    //                                    "local",
    //                                    "",
    //                                    1,
    //                                    ServerSoftware(
    //                                        "Purpur",
    //                                        "1.21.8",
    //                                        2493,
    //
    // "https://api.purpurmc.org/v2/purpur/1.21.8/2493/download",
    //                                        "plugins",
    //                                        SoftwareType.SERVER,
    //                                    ),
    //                                    emptyMap(),
    //                                    emptyList(),
    //                                    emptyList(),
    //                                    true,
    //                                )
    //                                .toDefinition()
    //                        )
    //                        .build()
    //                )
    //                .let {
    //                    Node.instance.localGrpcClient.serviceAPI.startService(
    //                        StartServiceRequest.newBuilder().setService(it.service).build()
    //                    )
    //                }
    //            source.sendMessage("Successfully started service on default task")
    //        }
    //    }
    //
    //    @Command("debug start service defaultproxy")
    //    fun startServiceOnDefaultTaskProxy(source: CommandSource) {
    //        NodeCoroutineScope.launch {
    //            source.sendMessage("Starting service on default proxy task")
    //            Node.instance.localGrpcClient.serviceAPI
    //                .prepareServiceByTask(
    //                    PrepareServiceByTaskRequest.newBuilder()
    //                        .setTask(
    //                            Task(
    //                                    "Proxy",
    //                                    512,
    //                                    512,
    //                                    28879,
    //                                    listOf(),
    //                                    true,
    //                                    1,
    //                                    1,
    //                                    false,
    //                                    false,
    //                                    "local",
    //                                    "",
    //                                    1,
    //                                    ServerSoftware(
    //                                        "Velocity",
    //                                        "3.4.0-SNAPSHOT",
    //                                        533,
    //
    // "https://fill-data.papermc.io/v1/objects/cb33a12f4b6057fe2f862212ab4c033202b86172527168a41e30a30c1d05d27e/velocity-3.4.0-SNAPSHOT-533.jar",
    //                                        "plugins",
    //                                        SoftwareType.PROXY,
    //                                    ),
    //                                    emptyMap(),
    //                                    emptyList(),
    //                                    emptyList(),
    //                                    false,
    //                                )
    //                                .toDefinition()
    //                        )
    //                        .build()
    //                )
    //                .let {
    //                    Node.instance.localGrpcClient.serviceAPI.startService(
    //                        StartServiceRequest.newBuilder().setService(it.service).build()
    //                    )
    //                }
    //            source.sendMessage("Successfully started service on default proxy task")
    //        }
    //    }
    //
    //    @Command("debug cmd")
    //    fun cmdCmd(source: CommandSource) {
    //        NodeCoroutineScope.launch {
    //            source.sendMessage("making command exec on proxy 1")
    //            DockerService(
    //                    Node.instance.nodeServices
    //                        .filterIsInstance<DockerService>()
    //                        .first { it.service.task.software.type == SoftwareType.PROXY }
    //                        .service
    //                )
    //                .command("velocity plugins")
    //        }
    //    }

    @Command("debug vconf create")
    fun createVConf(source: CommandSource) {
        NodeCoroutineScope.launch {
            source.sendMessage("Creating config")
            VirtualConfigDebugHelper.createDebugConfig()
            source.sendMessage("Successfully created config")
        }
    }

    @Command("debug vconf update")
    fun updateVConf(source: CommandSource) {
        NodeCoroutineScope.launch {
            source.sendMessage("Updating config")
            VirtualConfigDebugHelper.updateDebugConfig()
            source.sendMessage("Successfully updated config")
        }
    }

    @Command("debug findNextAvailableServiceOrderedId <startId>")
    fun findNextAvailableServiceOrderedId(
        source: CommandSource,
        @Argument("startId") startId: Int,
    ) {
        NodeCoroutineScope.launch {
            source.sendMessage("Finding next available service orderedId...")
            val id = Node.instance.serviceFactoryProvider
                .findServiceFactory("local")!!
                .findNextAvailableOrderedId(
                    Task.fromDefinition(
                        Node.instance.localGrpcClient.tasksAPI
                            .getByName(getByNameRequest { name = "Proxy" })
                            .task!!
                    ),
                    startId,
                )

            source.sendMessage("Next available service orderedId: $id")
        }
    }

    //    @Command("debug database main insert")
    //    fun debugInsertData(source: CommandSource) {
    //        NodeCoroutineScope.launch {
    //            source.sendMessage("Inserting data into database")
    //            Node.instance
    //                .getDatabaseProvider()
    //                .getOrCreateDatabase("debug")
    //                .upsert(debugProxyTask.name, Json.encodeToJsonElement(debugProxyTask))
    //            source.sendMessage("Successfully inserted data into database")
    //        }
    //    }
    //
    //    @Command("debug database main delete")
    //    fun debugDeleteData(source: CommandSource) {
    //        NodeCoroutineScope.launch {
    //            source.sendMessage("Deleting data from database")
    //            Node.instance
    //                .getDatabaseProvider()
    //                .getOrCreateDatabase("debug")
    //                .delete(debugProxyTask.name)
    //            source.sendMessage("Successfully deleted data from database")
    //        }
    //    }
    //
    //    @Command("debug database main get")
    //    fun debugGetData(source: CommandSource) {
    //        NodeCoroutineScope.launch {
    //            source.sendMessage("Getting data from database")
    //            val jsonElement =
    //                Node.instance
    //                    .getDatabaseProvider()
    //                    .getOrCreateDatabase("debug")
    //                    .get(debugProxyTask.name)
    //            source.sendMessage(jsonElement.toString())
    //            val task = Json.decodeFromJsonElement(Task.serializer(), jsonElement!!)
    //            source.sendMessage(task.toString())
    //        }
    //    }
}
