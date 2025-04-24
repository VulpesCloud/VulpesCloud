package de.vulpescloud.node.commands

import de.vulpescloud.api.service.Service
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.service.ServiceProviderImpl
import java.util.stream.Stream
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput

class ServiceCommand(serviceProvider: ServiceProvider) {
    private val serviceProvider = serviceProvider as ServiceProviderImpl

    @Suggestions("services")
    fun suggestServices(): Stream<String> {
        return serviceProvider.services().stream().map { it.name }
    }

    @Parser(suggestions = "services")
    fun serviceParser(input: CommandInput): Service {
        val command = input.readString()
        val service =
            serviceProvider.services().find { it.name.equals(command, true) }
                ?: throw IllegalArgumentException("Service does not exist!")

        return service
    }

    @Command("service|services <service>")
    fun showServiceInfo(source: CommandSource, @Argument("service") service: Service) {
        source.sendMessage("Service: ${service.name}")
        source.sendMessage("UUID: ${service.uuid}")
        source.sendMessage("State: ${service.state}")
        source.sendMessage("Version: ${service.task.version.name}-${service.task.version.version}")
        source.sendMessage("Node: ${service.runningNode.name}")
        source.sendMessage("PlayerCount: ${service.onlinePlayerCount}/${service.maxPlayers}")
        source.sendMessage("Port: ${service.port}")
    }

    @Command("service|services <service> start")
    fun startService(source: CommandSource, @Argument("service") service: Service) {
        if (service.state == ServiceStates.PREPARED) {
            val localService = serviceProvider.localServices.find { it.name == service.name }
            if (localService != null) {
                source.sendMessage("Starting service ${service.name}")
                localService.start()
            } else {
                source.sendMessage(
                    "REPORT THIS PLEASE! Starting a service from another node is not implemented yet!"
                )
            }
        } else {
            source.sendMessage("Service is already running!")
        }
    }

    @Command("service|services <service> stop")
    fun stopService(source: CommandSource, @Argument("service") service: Service, @Flag("force") force: Boolean) {
        TODO("Not yet implemented")
    }

    @Command("service|services <service> screen")
    fun toggleScreen(source: CommandSource, @Argument("service") service: Service) {
        TODO("Not yet implemented")
    }

}
