package de.vulpescloud.node.commands

import de.vulpescloud.node.Node
import de.vulpescloud.node.command.annotations.Alias
import de.vulpescloud.node.command.source.CommandSource
import org.incendo.cloud.annotations.Command

@Alias(["modules"])
class ModuleCommand {

    @Command("module|modules list")
    fun listModules(source: CommandSource) {
        Node.instance.moduleProvider.modules.forEach {
            source.sendMessage(" &8- &m${it.name} &8| &f${it.state} &8| &b${it.description}")
        }
    }

}