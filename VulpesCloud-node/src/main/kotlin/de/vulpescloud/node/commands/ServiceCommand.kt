package de.vulpescloud.node.commands

import de.vulpescloud.api.services.Service
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.source.CommandSource
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import java.util.stream.Stream

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

    @Command("service|services list")
    fun sendServiceHelp(source: CommandSource) {
        source.sendMessage("The following &b${Node.instance.serviceProvider.services().size} &7services are registered&8:")
        Node.instance.serviceProvider.services()
            .forEach { service -> source.sendMessage("&8- &f${service.name()} &8: (&7${service.details()}&8)") }
    }
}