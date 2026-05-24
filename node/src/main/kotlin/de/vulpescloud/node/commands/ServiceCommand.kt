package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.services.v1.DeleteServiceRequest
import build.buf.gen.vulpescloud.services.v1.GetAllServicesRequest
import build.buf.gen.vulpescloud.services.v1.ServiceDefinition
import build.buf.gen.vulpescloud.services.v1.StartServiceRequest
import build.buf.gen.vulpescloud.services.v1.StopServiceRequest
import build.buf.gen.vulpescloud.services.v1.getAllServicesRequest
import build.buf.gen.vulpescloud.services.v1.getLatestServiceSnapshotRequest
import com.github.benmanes.caffeine.cache.Caffeine
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.ConsoleCommandSource
import de.vulpescloud.node.command.annotation.Alias
import de.vulpescloud.node.services.ServiceLogHandler
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.processors.confirmation.annotation.Confirmation

@Alias(["ser"])
class ServiceCommand {

    @Suggestions("service")
    fun serviceSuggestions(): Stream<String> {
        return ServiceCache.getTasks().map { Service.fromDefinition(it).name() }.stream()
    }

    @Parser(suggestions = "service")
    fun serviceParser(input: CommandInput): List<Service> {
        val raw = input.readString()
        val pattern = raw.split("*").joinToString(".*") { Regex.escape(it) }
        val regex = Regex("^$pattern$", RegexOption.IGNORE_CASE)

        return ServiceCache.getTasks()
            .map { Service.fromDefinition(it) }
            .filter { regex.matches(it.name()) }
    }

    @Permission("services.getAll")
    @Command("services|ser list")
    fun listServices(source: CommandSource) {
        runBlocking {
            source.sendMessage("Listing all services...")
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(GetAllServicesRequest.newBuilder().build())
                .servicesList
                .forEach {
                    source.sendMessage(
                        "Service: <white>${it.task.name}-${it.orderedId} <dark_gray>- <gray>${it.state.name} <dark_gray>- <gray>${it.port}"
                    )
                }
        }
    }

    @Permission("services.get")
    @Command("services|service <service> info")
    fun infoService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            source.sendMessage(
                "<gray>Name: <yellow>${it.name()} \n" +
                    "<gray>UUID: <yellow>${it.uuid} \n" +
                    "<gray>Port: <yellow>${it.port} \n" +
                    "<gray>Node: <yellow>${it.node} \n" +
                    "<gray>PlayerCount: <yellow>${it.playerCount} \n" +
                    "<dark_gray>State: <yellow>${it.state}"
            )
        }
    }

    @Permission("services.start")
    @Command("services|ser <service> start")
    fun startService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            if (it.state == ServiceStates.STARTING || it.state == ServiceStates.RUNNING) {
                source.sendMessage("Service is already running!")
                return
            }
            runBlocking {
                source.sendMessage("Starting service <white>${it.task.name}-${it.orderedId}...")
                Node.instance.localGrpcClient.serviceAPI.startService(
                    StartServiceRequest.newBuilder().setService(it.toDefinition()).build()
                )
            }
        }
    }

    @Permission("services.stop")
    @Command("services|ser <service> stop")
    fun stopService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            runBlocking {
                Node.instance.localGrpcClient.serviceAPI.stopService(
                    StopServiceRequest.newBuilder().setService(it.toDefinition()).build()
                )
            }
        }
    }

    @Permission("services.getSnapshot")
    @Command("services|ser <service> snapshot")
    fun getSnapshot(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            runBlocking {
                source.sendMessage(
                    "Retrieving snapshot for service <white>${it.task.name}-${it.orderedId}..."
                )
                val snapshot =
                    Node.instance.localGrpcClient.serviceAPI
                        .getLatestServiceSnapshot(
                            getLatestServiceSnapshotRequest { this.service = it.toDefinition() }
                        )
                        .snapshot
                source.sendMessage(
                    "<gray>Snapshot for <yellow>${it.task.name}-${it.orderedId}:\n" +
                        "<gray>Timestamp: <yellow>${snapshot.snapshotTime}\n" +
                        "<gray>Players: <yellow>${snapshot.playerCount}\n" +
                        "<gray>System CPU Usage: <yellow>${snapshot.systemCpuUsage}\n" +
                        "<gray>Process CPU Usage: <yellow>${snapshot.cpuUsage}\n" +
                        "<gray>Max Heap Memory: <yellow>${snapshot.maxHeapMemory}\n" +
                        "<gray>Heap Memory Usage: <yellow>${snapshot.heapUsageMemory}\n" +
                        "<gray>Non Heap Memory Usage: <yellow>${snapshot.noHeapUsageMemory}\n" +
                        "<gray>Uptime: <yellow>${snapshot.uptimeMillis.toDuration(DurationUnit.MILLISECONDS)}"
                )
            }
        }
    }

    @Permission("services.delete")
    @Command("services|ser <service> delete")
    fun deleteService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            runBlocking {
                Node.instance.localGrpcClient.serviceAPI.deleteService(
                    DeleteServiceRequest.newBuilder().setService(it.toDefinition()).build()
                )
            }
        }
    }

    @Permission("services.stopAll")
    @Confirmation
    @Command("services|ser stopAll")
    fun stopAllService(source: CommandSource) {
        runBlocking {
            source.sendMessage("Stopping all services...")
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(GetAllServicesRequest.newBuilder().build())
                .servicesList
                .forEach {
                    Node.instance.localGrpcClient.serviceAPI.stopService(
                        StopServiceRequest.newBuilder().setService(it).build()
                    )
                }
        }
    }

    @Permission("services.deleteAll")
    @Confirmation
    @Command("services|ser deleteAll")
    fun deleteAllServices(source: CommandSource) {
        runBlocking {
            source.sendMessage("Deleting all services...")
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(GetAllServicesRequest.newBuilder().build())
                .servicesList
                .forEach {
                    Node.instance.localGrpcClient.serviceAPI.deleteService(
                        DeleteServiceRequest.newBuilder().setService(it).build()
                    )
                }
        }
    }

    @Permission("services.sendCommand")
    @Command("services|ser <service> command <command>")
    fun sendCommand(
        source: CommandSource,
        @Argument("service") service: List<Service>,
        @Greedy @Argument("command") command: String,
    ) {
        runBlocking {
            service.forEach {
                val resp =
                    Node.instance.localGrpcClient.serviceAPI.sendCommand(
                        build.buf.gen.vulpescloud.services.v1.sendCommandRequest {
                            this.service = it.toDefinition()
                            this.command = command
                        }
                    )
                if (resp.success) {
                    source.sendMessage(
                        "Sent command to service <white>${it.task.name}-${it.orderedId}<dark_gray>."
                    )
                } else {
                    source.sendMessage(
                        "Failed to send command to service <white>${it.task.name}-${it.orderedId}<dark_gray>."
                    )
                }
            }
        }
    }

    @Command("services|ser <service> screen")
    fun toggleScreen(source: CommandSource, @Argument("service") service: List<Service>) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        service.forEach {
            ServiceLogHandler.toggleServiceLogging("${it.task.name}-${it.orderedId}")
        }
    }
}

object ServiceCache {
    private val cache =
        Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build<String, List<ServiceDefinition>>()

    fun getTasks(): List<ServiceDefinition> {
        return cache.get("tasks") {
            runBlocking {
                Node.instance.localGrpcClient.serviceAPI
                    .getAllServices(getAllServicesRequest {})
                    .servicesList
            }
        }
    }
}
