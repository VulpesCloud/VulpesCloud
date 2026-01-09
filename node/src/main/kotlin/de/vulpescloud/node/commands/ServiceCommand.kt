package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.services.v1.DeleteServiceRequest
import build.buf.gen.vulpescloud.services.v1.GetAllServicesRequest
import build.buf.gen.vulpescloud.services.v1.StartServiceRequest
import build.buf.gen.vulpescloud.services.v1.StopServiceRequest
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotation.Alias
import de.vulpescloud.node.services.ServiceLogHandler
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.processors.confirmation.annotation.Confirmation

@Alias(["ser"])
class ServiceCommand {

    @Suggestions("services")
    fun serviceSuggestions(): Stream<String> {
        return CompletableFuture.supplyAsync {
                runBlocking {
                    Node.instance.localGrpcClient.serviceAPI
                        .getAllServices(GetAllServicesRequest.newBuilder().build())
                        .servicesList
                        .filter { it != null && it.hasTask() && !it.task.name.isNullOrEmpty() }
                        .map { "${it.task.name}-${it.orderedId}" }
                }
            }
            .thenApply { it.stream() }
            .exceptionally { Stream.empty() }
            .get(5, TimeUnit.SECONDS)
    }

    @Parser(suggestions = "services")
    fun serviceParser(input: CommandInput): List<Service>? {
        return CompletableFuture.supplyAsync {
                runBlocking {
                    val regexPattern = input.readString()
                    regexPattern.replace("*", ".*")
                    val regex = Regex(regexPattern)

                    Node.instance.localGrpcClient.serviceAPI
                        .getAllServices(GetAllServicesRequest.newBuilder().build())
                        .servicesList
                        .filter { regex.matches("${it.task.name}-${it.orderedId}") }
                        .map { Service.fromDefinition(it) }
                }
            }
            .thenApply { it }
            .exceptionally { throw it }
            .get(5, TimeUnit.SECONDS)
    }

    @Command("services|ser list")
    fun listServices(source: CommandSource) {
        NodeCoroutineScope.launch {
            source.sendMessage("Listing all services...")
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(GetAllServicesRequest.newBuilder().build())
                .servicesList
                .forEach {
                    source.sendMessage(
                        "Service: &f${it.task.name}-${it.orderedId} &8- &7${it.state.name} &8- &7${it.port}"
                    )
                }
        }
    }

    @Command("services|service <service> info")
    fun infoService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            source.sendMessage(
                "Service: &f${it.task.name}-${it.orderedId} &8- &7${it.state.name} &8- &7${it.port}"
            )
        }
    }

    @Command("services|ser <service> start")
    fun startService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            if (it.state == ServiceStates.STARTING || it.state == ServiceStates.RUNNING) {
                source.sendMessage("Service is already running!")
                return
            }
            NodeCoroutineScope.launch {
                source.sendMessage("Starting service &f${it.task.name}-${it.orderedId}...")
                Node.instance.localGrpcClient.serviceAPI.startService(
                    StartServiceRequest.newBuilder().setService(it.toDefinition()).build()
                )
            }
        }
    }

    @Command("services|ser <service> stop")
    fun stopService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            NodeCoroutineScope.launch {
                Node.instance.localGrpcClient.serviceAPI.stopService(
                    StopServiceRequest.newBuilder().setService(it.toDefinition()).build()
                )
            }
        }
    }

    @Command("services|ser <service> delete")
    fun deleteService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            NodeCoroutineScope.launch {
                Node.instance.localGrpcClient.serviceAPI.deleteService(
                    DeleteServiceRequest.newBuilder().setService(it.toDefinition()).build()
                )
            }
        }
    }

    @Confirmation
    @Command("services|ser stopAll")
    fun stopAllService(source: CommandSource) {
        NodeCoroutineScope.launch {
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

    @Confirmation
    @Command("services|ser deleteAll")
    fun deleteAllServices(source: CommandSource) {
        NodeCoroutineScope.launch {
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

    @Command("services|ser <service> command <command>")
    fun sendCommand(
        source: CommandSource,
        @Argument("service") service: List<Service>,
        @Greedy @Argument("command") command: String,
    ) {
        NodeCoroutineScope.launch {
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
                        "Sent command to service &f${it.task.name}-${it.orderedId}&8."
                    )
                } else {
                    source.sendMessage(
                        "Failed to send command to service &f${it.task.name}-${it.orderedId}&8."
                    )
                }
            }
        }
    }

    @Command("services|ser <service> screen")
    fun toggleScreen(@Argument("service") service: List<Service>) {
        service.forEach {
            ServiceLogHandler.toggleServiceLogging("${it.task.name}-${it.orderedId}")
        }
    }
}
