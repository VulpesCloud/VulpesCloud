package de.vulpescloud.connector.velocity.commands

import de.vulpescloud.bridge.task.TaskProvider
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import net.kyori.adventure.text.minimessage.MiniMessage

class CloudCommand {

    val miniMessage = MiniMessage.miniMessage()

    val command = commandTree("cloud") {
        literalArgument("task") {
            withAliases("tasks")
            withPermission("vulpescloud.task")
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
        }
    }
}