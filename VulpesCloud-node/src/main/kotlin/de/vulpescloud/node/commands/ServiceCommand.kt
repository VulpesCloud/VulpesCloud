package de.vulpescloud.node.commands

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.api.service.ServiceActions
import de.vulpescloud.api.service.ServiceInfo
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotations.Description
import de.vulpescloud.node.service.ServiceLogBuffer
import de.vulpescloud.node.service.ServiceProviderImpl
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.json.JSONObject
import java.util.stream.Stream

@Suppress("Unused")
@Description("COMMANDS.DESCRIPTION.services")
class ServiceCommand(
    serviceProvider: ServiceProvider,
    private val clusterProvider: ClusterProvider,
) {
    private val serviceProvider = serviceProvider as ServiceProviderImpl

    @Suggestions("services")
    fun suggestServices(): Stream<String> {
        return serviceProvider.services().stream().map { it.name }
    }

    @Parser(suggestions = "services")
    fun serviceParser(input: CommandInput): List<ServiceInfo> {
        val regexPattern = input.readString()
        regexPattern.replace("*", ".*")
        val regex = Regex(regexPattern)
        return serviceProvider.services().filter { regex.matches(it.name) }
    }

    @Command("service|services <services>")
    fun showServiceInfo(source: CommandSource, @Argument("services") services: List<ServiceInfo>) {
        services.forEach { serviceInfo ->
            source.sendMessage("Service: ${serviceInfo.name}")
            source.sendMessage("UUID: ${serviceInfo.uuid}")
            source.sendMessage("State: ${serviceInfo.state}")
            source.sendMessage(
                "Version: ${serviceInfo.task.version.name}-${serviceInfo.task.version.version}"
            )
            source.sendMessage("Node: ${serviceInfo.runningNode.name}")
            source.sendMessage(
                "PlayerCount: ${serviceInfo.onlinePlayerCount}/${serviceInfo.maxPlayers}"
            )
            source.sendMessage("Port: ${serviceInfo.port}")
            source.sendMessage("------------------------")
        }
    }

    @Command("service|services list")
    fun listServices(source: CommandSource) {
        val services = serviceProvider.services()
        val maxNameLength = services.maxOfOrNull { it.name.length } ?: 0
        source.sendMessage("A total of ${services.size} service(s) are available:")
        services.forEach {
            val paddedName = it.name.padEnd(maxNameLength)
            source.sendMessage(
                " &8- &m$paddedName &7State: &e${it.state}&8, &7OnlinePlayerCount: &e${it.onlinePlayerCount}&8, &7MaxPlayers: &e${it.maxPlayers}&8, &7RunningNode: &e${it.runningNode.name}&8, &7Port: &e${it.port}"
            )
        }
    }

    @Command("service|services <services> start")
    fun startService(source: CommandSource, @Argument("services") services: List<ServiceInfo>) {
        services.forEach { serviceInfo ->
            if (serviceInfo.state == ServiceStates.PREPARED) {
                val localService =
                    serviceProvider.localServices.find { it.name == serviceInfo.name }
                if (localService != null) {
                    source.sendMessage("Starting service ${serviceInfo.name}")
                    localService.start()
                } else {
                    source.sendMessage(
                        "Notifying ${serviceInfo.runningNode.name} to start service ${serviceInfo.name}"
                    )
                    getRC()
                        ?.sendMessage(
                            JSONObject()
                                .put("receiver", serviceInfo.runningNode.name)
                                .put("sender", clusterProvider.localNode().name)
                                .put("content", "SERVICE")
                                .put("action", ServiceActions.START)
                                .put("service", serviceInfo.name)
                                .toString(),
                            RedisChannels.VULPESCLOUD_NODE_COMMUNICATION.name,
                        )
                }
            } else {
                source.sendMessage("Service ${serviceInfo.name} is already running!")
            }
        }
    }

    @Command("service|services <services> stop")
    fun stopService(
        source: CommandSource,
        @Argument("services") services: List<ServiceInfo>,
        @Flag("force") force: Boolean,
    ) {
        services.forEach { serviceInfo ->
            if (force) {
                source.sendMessage(
                    "Notifying ${serviceInfo.runningNode.name} to kill service ${serviceInfo.name}"
                )
                getRC()
                    ?.sendMessage(
                        JSONObject()
                            .put("receiver", serviceInfo.runningNode.name)
                            .put("sender", clusterProvider.localNode().name)
                            .put("content", "SERVICE")
                            .put("action", ServiceActions.KILL)
                            .put("service", serviceInfo.name)
                            .toString(),
                        RedisChannels.VULPESCLOUD_NODE_COMMUNICATION.name,
                    )
            } else {
                source.sendMessage(
                    "Notifying ${serviceInfo.runningNode.name} to stop service ${serviceInfo.name}"
                )
                getRC()
                    ?.sendMessage(
                        JSONObject()
                            .put("receiver", serviceInfo.runningNode.name)
                            .put("sender", clusterProvider.localNode().name)
                            .put("content", "SERVICE")
                            .put("action", ServiceActions.STOP)
                            .put("service", serviceInfo.name)
                            .toString(),
                        RedisChannels.VULPESCLOUD_NODE_COMMUNICATION.name,
                    )
            }
        }
    }

    @Command("service|services <services> screen")
    fun toggleScreen(source: CommandSource, @Argument("services") services: List<ServiceInfo>) {
        services.forEach { serviceInfo ->
            if (serviceProvider.toggleServiceLogging(serviceInfo)) {
                ServiceLogBuffer.getLogs(serviceInfo.name).forEach { line ->
                    source.sendMessage("&8[ &m${serviceInfo.name} &8] &b$line")
                }
                source.sendMessage("Enabled screen logging for ${serviceInfo.name}")
            } else {
                source.sendMessage("Disabled screen logging for ${serviceInfo.name}")
            }
        }
    }

    @Command("service|services <services> command <command>")
    fun sendCommandToService(
        source: CommandSource,
        @Argument("services") services: List<ServiceInfo>,
        @Greedy @Argument("command") command: String,
    ) {
        services.forEach { serviceInfo ->
            source.sendMessage(
                "Notifying ${serviceInfo.runningNode.name} to send the command to service ${serviceInfo.name}"
            )
            getRC()
                ?.sendMessage(
                    JSONObject()
                        .put("receiver", serviceInfo.runningNode.name)
                        .put("sender", clusterProvider.localNode().name)
                        .put("content", "SERVICE")
                        .put("action", ServiceActions.COMMAND.name)
                        .put("service", serviceInfo.name)
                        .put("command", command)
                        .toString(),
                    RedisChannels.VULPESCLOUD_NODE_COMMUNICATION.name,
                )
        }
    }
}
