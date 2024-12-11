package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.annotations.Alias
import de.vulpescloud.node.command.annotations.Description
import de.vulpescloud.node.command.source.CommandSource
import de.vulpescloud.node.services.ServiceFactory
import de.vulpescloud.node.setups.TaskSetup
import de.vulpescloud.node.tasks.TaskImpl
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.jetbrains.annotations.Nullable
import java.util.stream.Stream

@Description("Manage all Tasks (translate)")
@Alias(["tasks"])
class TaskCommand {

    @Parser(suggestions = "tasks")
    fun taskParser(input: CommandInput): Task {
        val command = input.readString()
        val task = Node.instance.taskProvider.tasks().find { it.name().equals(command, true) } ?: throw IllegalArgumentException()

        return task
    }

    @Suggestions("tasks")
    fun suggestTasks(): Stream<String> {
        return Node.instance.taskProvider.tasks().stream().map { it.name() }
    }

    @Command("task|tasks list")
    fun listAllTasks(
        source: CommandSource
    ) {
        val tasks = Node.instance.taskProvider.tasks()
        source.sendMessage("Following &b${tasks.size} &7tasks are registered&8:")
        tasks.forEach { source.sendMessage(" - ${it.name()}") }
    }

    @Command("task|tasks setup")
    fun startSetup() {
        Node.instance.setupProvider.startSetup(TaskSetup())
    }

    @Command("task|tasks <task>")
    fun sendTaskInfo(
        source: CommandSource,
        @Argument("task") task: Task
    ) {
        source.sendMessage("Name: ${task.name()}")
        source.sendMessage("Templates: ${task.templates()}")
    }

    @Command("task|tasks <task> prepare")
    fun startOrPrepareServiceOnTask(
        source: CommandSource,
        @Argument("task") task: Task,
        @Flag("start") startService: Boolean,
    ) {
        if (startService) {
            source.sendMessage("Starting and preparing Service on Task: ${task.name()}")
            ServiceFactory.prepareStartedService(task)
        } else {
            source.sendMessage("Preparing Service on Task: ${task.name()}")
            source.sendMessage("NOTE: You will have to start this Service yourself!")
            ServiceFactory.prepareService(task)
        }
    }

}