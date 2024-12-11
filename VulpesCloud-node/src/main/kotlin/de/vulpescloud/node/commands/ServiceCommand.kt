package de.vulpescloud.node.commands

import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceActions
import de.vulpescloud.api.services.builder.ServiceActionMessageBuilder
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.source.CommandSource
import de.vulpescloud.node.json.ServiceSerializer.jsonFromService
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import java.util.stream.Stream

@Suppress("UNUSED")
class ServiceCommand {

    @Parser(suggestions = "services")
    fun serviceParser(input: CommandInput): Service {
        val command = input.readString()
        val task = Node.instance.serviceProvider.findServiceByName(command) ?: throw IllegalArgumentException()

        return task
    }

    @Suggestions("services")
    fun suggestServices(): Stream<String> {
        return Node.instance.serviceProvider.services().stream().map { it.name() }
    }

    @Command("service|services|ser list")
    fun sendServiceHelp(source: CommandSource) {
        source.sendMessage("The following &b${Node.instance.serviceProvider.services().size} &7services are registered&8:")
        Node.instance.serviceProvider.services()
            .forEach { service -> source.sendMessage("&8- &f${service.name()} &8: (&7${service.details()}&8)") }
    }

    @Command("service|services|ser stopAll")
    fun stopAll(source: CommandSource) {
        source.sendMessage("Stopping all Services!")
        Node.instance.serviceProvider.services()
           .forEach {
                Node.instance.getRC()?.sendMessage(
                    ServiceActionMessageBuilder
                       .setService(it)
                       .setAction(ServiceActions.STOP)
                       .build(),
                    RedisChannelNames.VULPESCLOUD_SERVICE_ACTION.name
                )
           }
    }

    @Command("service|services|ser <service> stop")
    fun sendServiceInfo(
        source: CommandSource,
        @Argument("service") service: Service
    ) {
        source.sendMessage("Trying to stop the Service!")
        Node.instance.getRC()?.sendMessage(
            ServiceActionMessageBuilder
                .setService(service)
                .setAction(ServiceActions.STOP)
                .build(),
            RedisChannelNames.VULPESCLOUD_SERVICE_ACTION.name
        )
    }

    @Command("service|services|ser <service> screen")
    fun toggleScreen(
        source: CommandSource,
        @Argument("service") service: Service
    ) {
        val ser = Node.instance.serviceProvider.findServiceById(service.id())!!
        if (Node.instance.serviceProvider.isLogging(ser)) {
            source.sendMessage("Disabling Screen for &m${service.name()}")
            Node.instance.serviceProvider.loggingServices.remove(ser.name())
        } else {
            source.sendMessage("Enabling Screen for &m${service.name()}")
            Node.instance.serviceProvider.loggingServices.add(ser.name())
        }
    }
}