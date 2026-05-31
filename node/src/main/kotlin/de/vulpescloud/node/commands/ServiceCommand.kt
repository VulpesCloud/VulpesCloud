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
import de.vulpescloud.node.command.annotation.SpecificCommandSource
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
            source.sendMessage("<gray>Listing all services...</gray>")
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(GetAllServicesRequest.newBuilder().build())
                .servicesList
                .forEach {
                    source.sendMessage(
                        " <dark_gray>»</dark_gray> <white>${it.task.name}-${it.orderedId}</white> <dark_gray>| <gray>State:</gray> <white>${it.state.name}</white> <dark_gray>| <gray>Port:</gray> <white>${it.port}</white>"
                    )
                }
        }
    }

    @Permission("services.get")
    @Command("services|service <service> info")
    fun infoService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            source.sendMessage(
                "<gold>---------</gold> <white>${it.name()}</white> <gold>---------</gold>\n" +
                        "<gray>UUID<dark_gray>:</dark_gray> <white>${it.uuid}</white>\n" +
                        "<gray>Port<dark_gray>:</dark_gray> <white>${it.port}</white>\n" +
                        "<gray>Node<dark_gray>:</dark_gray> <white>${it.node}</white>\n" +
                        "<gray>Players<dark_gray>:</dark_gray> <white>${it.playerCount}</white>\n" +
                        "<gray>State<dark_gray>:</dark_gray> <white>${it.state}</white>"
            )
        }
    }

    @Permission("services.start")
    @Command("services|ser <service> start")
    fun startService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            if (it.state == ServiceStates.STARTING || it.state == ServiceStates.RUNNING) {
                source.sendMessage("<red>Service is already running!</red>")
                return
            }
            runBlocking {
                source.sendMessage("<gray>Starting service</gray> <white>${it.task.name}-${it.orderedId}</white><gray>...</gray>")
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
                    "<gray>Retrieving snapshot for service</gray> <white>${it.task.name}-${it.orderedId}</white><gray>...</gray>"
                )
                val snapshot =
                    Node.instance.localGrpcClient.serviceAPI
                        .getLatestServiceSnapshot(
                            getLatestServiceSnapshotRequest { this.service = it.toDefinition() }
                        )
                        .snapshot
                source.sendMessage(
                    "<gold>---------</gold> <white>${it.task.name}-${it.orderedId}</white> <gold>---------</gold>\n" +
                        "<gray>Timestamp<dark_gray>:</dark_gray> <white>${snapshot.snapshotTime}</white>\n" +
                        "<gray>Players<dark_gray>:</dark_gray> <white>${snapshot.playerCount}</white>\n" +
                        "<gray>System CPU<dark_gray>:</dark_gray> <white>${snapshot.systemCpuUsage}%</white>\n" +
                        "<gray>Process CPU<dark_gray>:</dark_gray> <white>${snapshot.cpuUsage}%</white>\n" +
                        "<gray>Max Memory<dark_gray>:</dark_gray> <white>${snapshot.maxHeapMemory}mb</white>\n" +
                        "<gray>Heap Usage<dark_gray>:</dark_gray> <white>${snapshot.heapUsageMemory}mb</white>\n" +
                        "<gray>Non-Heap Usage<dark_gray>:</dark_gray> <white>${snapshot.noHeapUsageMemory}mb</white>\n" +
                        "<gray>Uptime<dark_gray>:</dark_gray> <white>${snapshot.uptimeMillis.toDuration(DurationUnit.MILLISECONDS)}</white>"
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
            source.sendMessage("<gray>Stopping all services...</gray>")
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
            source.sendMessage("<gray>Deleting all services...</gray>")
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
                        "<gray>Sent command to service</gray> <white>${it.task.name}-${it.orderedId}</white><dark_gray>.</dark_gray>"
                    )
                } else {
                    source.sendMessage(
                        "<red>Failed to send command to service</red> <white>${it.task.name}-${it.orderedId}</white><dark_gray>.</dark_gray>"
                    )
                }
            }
        }
    }

    @SpecificCommandSource(ConsoleCommandSource::class)
    @Command("services|ser <service> screen")
    fun toggleScreen(source: CommandSource, @Argument("service") service: List<Service>) {
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
