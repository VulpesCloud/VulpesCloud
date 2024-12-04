package de.vulpescloud.node.commands

import de.vulpescloud.node.Node
import de.vulpescloud.node.command.annotations.Alias
import de.vulpescloud.node.command.annotations.Description
import de.vulpescloud.node.command.source.CommandSource
import org.incendo.cloud.annotations.Command

@Description("Manage all Tasks (translate)")
@Alias(["tasks"])
class TaskCommand {

    @Command("task|tasks list")
    fun listAllTasks(
        source: CommandSource
    ) {
        val tasks = Node.instance.taskProvider.tasks()
        source.sendMessage("Following &b${tasks.size} &7tasks are registered&8:")
        tasks.forEach { source.sendMessage(" - ${it.name()}") }
    }

}