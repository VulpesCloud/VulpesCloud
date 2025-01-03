package de.vulpescloud.connector.velocity.commands

import de.vulpescloud.bridge.task.TaskProvider
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.textArgument
import net.kyori.adventure.text.minimessage.MiniMessage
import kotlin.math.min

class CloudCommand {

    val miniMessage = MiniMessage.miniMessage()

    val command = commandTree("cloud") {
        /*
        TASKS
         */
        literalArgument("task") {
            withAliases("tasks")
            withPermission("vulpescloud.task")
                    /*
                TASKS LIST
                    */
            literalArgument("list") {
                executes(
                    CommandExecutor { sender, _ ->
                        val tasks = TaskProvider.tasks()
                        sender.sendMessage(
                            miniMessage.deserialize("<gray>Following <aqua>${tasks.size}</aqua> tasks are registered:</gray>")
                        )
                        tasks.forEach {
                            sender.sendMessage(
                                miniMessage.deserialize("<gray>-</gray> <aqua>${it.name()}</aqua> <gray>[ <white>services<gray>:</gray> <green>${it.serviceCount()}</green> startPort<gray>:</gray> <green>${it.startPort()}</green> static<gray>:</gray> <green>${it.staticService()}</green></white> ]</gray>")
                            )
                        }
                    }
                )
            }
            textArgument("task", false) {
                literalArgument("info") {
                    executes(
                        CommandExecutor { sender, args ->
                            val task = TaskProvider.tasks().find { it.name() == args[0] }
                            if (task == null) {
                                sender.sendMessage(miniMessage.deserialize("<red>Task not found.</red>"))
                            } else {
                                sender.sendMessage(miniMessage.deserialize("Name: ${task.name()}"))
                                sender.sendMessage(miniMessage.deserialize("Nodes: ${task.nodes()}"))
                                sender.sendMessage(miniMessage.deserialize("Templates: ${task.templates()}"))
                                sender.sendMessage(miniMessage.deserialize("maxMemory: ${task.maxMemory()}"))
                                sender.sendMessage(miniMessage.deserialize("Max Players: ${task.maxPlayers()}"))
                                sender.sendMessage(miniMessage.deserialize("Static Services: ${task.staticService()}"))
                                sender.sendMessage(miniMessage.deserialize("Min Online Count: ${task.minOnlineCount()}"))
                                sender.sendMessage(miniMessage.deserialize("Maintenance: ${task.maintenance()}"))
                                sender.sendMessage(miniMessage.deserialize("Start Port: ${task.startPort()}"))
                                sender.sendMessage(miniMessage.deserialize("Fallback: ${task.fallback()}"))
                                sender.sendMessage(miniMessage.deserialize("ServiceCount: ${task.serviceCount()}"))
                                sender.sendMessage(miniMessage.deserialize("Version: ${task.version()}"))
                            }
                        }
                    )
                }
            }
        }
    }
}