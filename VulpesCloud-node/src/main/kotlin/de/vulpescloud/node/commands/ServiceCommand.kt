package de.vulpescloud.node.commands

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.api.service.Service
import de.vulpescloud.api.service.ServiceActions
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.service.ServiceProviderImpl
import java.util.stream.Stream
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.json.JSONObject

@Suppress("Unused")
class ServiceCommand(serviceProvider: ServiceProvider, private val clusterProvider: ClusterProvider) {
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
                source.sendMessage("Notifying ${service.runningNode.name} to start service ${service.name}")
                getRC()?.sendMessage(RedisChannels.VULPESCLOUD_NODE_COMMUNICATION.name,
                    JSONObject()
                        .put("receiver", service.runningNode.name)
                        .put("sender", clusterProvider.localNode().name)
                        .put("content", "SERVICE")
                        .put("action", ServiceActions.START)
                        .put("service", service.name)
                        .toString()
                )
            }
        } else {
            source.sendMessage("Service is already running!")
        }
    }

    @Command("service|services <service> stop")
    fun stopService(source: CommandSource, @Argument("service") service: Service, @Flag("force") force: Boolean) {
        if (force) {
            source.sendMessage("Notifying ${service.runningNode.name} to kill service ${service.name}")
            getRC()?.sendMessage(RedisChannels.VULPESCLOUD_NODE_COMMUNICATION.name,
                JSONObject()
                    .put("receiver", service.runningNode.name)
                    .put("sender", clusterProvider.localNode().name)
                    .put("content", "SERVICE")
                    .put("action", ServiceActions.KILL)
                    .put("service", service.name)
                    .toString()
            )
        } else {
            source.sendMessage("Notifying ${service.runningNode.name} to stop service ${service.name}")
            getRC()?.sendMessage(RedisChannels.VULPESCLOUD_NODE_COMMUNICATION.name,
                JSONObject()
                    .put("receiver", service.runningNode.name)
                    .put("sender", clusterProvider.localNode().name)
                    .put("content", "SERVICE")
                    .put("action", ServiceActions.STOP)
                    .put("service", service.name)
                    .toString()
            )
        }

    }

    @Command("service|services <service> screen")
    fun toggleScreen(source: CommandSource, @Argument("service") service: Service) {
        TODO("Not yet implemented")
    }

}
