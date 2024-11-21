package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.node.Node
import de.vulpescloud.node.setups.TaskSetup
import de.vulpescloud.node.task.TaskImpl
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.StringParser
import org.json.JSONObject
import org.slf4j.LoggerFactory

class TasksCommand {
    val logger = LoggerFactory.getLogger(TasksCommand::class.java)

    init {
        Node.commandProvider?.registeredCommands?.add(
            CommandInfo(
                "task",
                setOf("tasks"),
                "Clear the Terminal.",
                listOf("")
            )
        )

        Node.commandProvider!!.commandManager!!.buildAndRegister("task", aliases = arrayOf("tasks")) {
            literal("list")

            handler { _ ->
                logger.info("Following &b${Node.taskProvider.tasks()?.size} &7tasks are loaded&8:")
                Node.taskProvider.tasks()
                    ?.forEach { task -> logger.info("&8- &f${task.name()}&8: (&7${task.details()}&8)") }
            }
        }

        Node.commandProvider!!.commandManager!!.buildAndRegister("task", aliases = arrayOf("tasks")) {
            literal("task")
            required("task", StringParser.stringParser(StringParser.StringMode.SINGLE))
            literal("start")

            handler { ctx ->
                val task = Node.taskProvider.findByName(ctx.get<String>("task").toString())

                if (task != null) {
                    logger.info("Staring a Service!")
                    Node.serviceProvider.factory().startServiceOnTask(task)

                } else {
                    logger.info("The Task that has been entered is not valid!")
                }
            }
        }

        Node.commandProvider!!.commandManager!!.buildAndRegister("task", aliases = arrayOf("tasks")) {
            literal("task")
            required("task", StringParser.stringParser(StringParser.StringMode.SINGLE))
            literal("delete")

            handler { ctx ->
                val task = Node.taskProvider.findByName(ctx.get<String>("task").toString())

                if (Node.taskProvider.exists(ctx.get<String>("task").toString())) {
                    Node.taskProvider.delete(task!!.name())
                    logger.info("Deleting the Task!")
                } else {
                    logger.info("The Task that has been entered is not valid!")
                }
            }
        }

        Node.commandProvider!!.commandManager!!.buildAndRegister("task", aliases = arrayOf("tasks")) {
            literal("setup")

            handler { _ ->
                Node.setupProvider.startSetup(TaskSetup())
            }
        }

        Node.commandProvider!!.commandManager!!.buildAndRegister("task", aliases = arrayOf("tasks")) {
            literal("reload")

            handler { _ ->
                logger.info("Reloading Tasks!")
                Node.taskProvider.reload()
            }
        }


    }
}


////class TasksCommand : Command("group", "Manage or create your cluster groups", "groups") {
////
////    val logger = Logger()
////
////    init {
////        val groupService = Node.taskProvider
////
////        // argument for group name
////        val groupArgument = CommandArgumentType.ClusterTask("task")
////
////        syntax  ({ context ->
////            logger.info("Following &b${groupService?.groups()?.size} &7groups are loaded&8:")
////            groupService?.groups()?.forEach { group -> logger.info("&8- &f${group.name()}&8: (&7${group.details()}&8)") }
////        }, "List all registered groups&8.", CommandArgumentType.Keyword("list"))
////
////        syntax({ context -> TaskSetup().run() }, "Create a new cluster group.", CommandArgumentType.Keyword("create"))
////
////        syntax({ context ->
////            groupService?.delete(context.arg(groupArgument))
////                ?.ifPresentOrElse(
////                    { s -> logger.warn("Cannot delete group: $s") },
////                    { logger.info("Successfully delete group ${context.arg(groupArgument)} in cluster!") }
////                )
////        }, "Delete the selected group&8.", groupArgument, CommandArgumentType.Keyword("delete"))
//
////        syntax({ context ->
////            val group = context.arg(groupArgument)
////            logger.info("Name&8: &b{}", group.name())
////            logger.info("Runtime nodes&8: &b{}", String.join("&8, &b", group.nodes()))
////            logger.info("Platform&8: &b{}", group.platform().details())
////            logger.info("Static service&8: &b{}", group.staticService())
////            logger.info("Maximum memory&8: &b{}mb", group.maxMemory())
////            logger.info("Minimum online services&8: &b{}", group.minOnlineServerInstances())
////            logger.info("Maximum online services&8: &b{}", group.maxOnlineServerInstances())
////            logger.info("Maximal players: &b{}", group.maxPlayers())
////            logger.info("Properties&8(&b{}&8):", group.properties().pool().size())
////        }, "Show all information about a group&8.", groupArgument, Keyword("info"))
////
////        syntax({ context ->
////            val group: Unit = context.arg(groupArgument)
////            if (group.services().isEmpty()) {
////                log.info("This group has no services&8!")
////                return@syntax
////            }
////
////            log.info("Stopping all services of the group &8'&f{}&8'...", group.name())
////            for (service in group.services()) {
////                service.shutdown()
////            }
////        }, "Shutdown all services with this group&8.", groupArgument, Keyword("shutdown"))
////
////
////        val editKey = Enum<E>(
////            ClusterGroupEditFields::class.java, "key"
////        )
////        val editValue = Text("value")
////
////        syntax({ context ->
////            val group: Unit = context.arg(groupArgument)
////            val editableField: Unit = context.arg(editKey)
////            val value: Unit = context.arg(editValue)
////            log.info("{}:{}: {}", group.name(), editableField.name(), value)
////        }, "Change a property of a group&8.", groupArgument, Keyword("edit"), editKey, editValue)
////    }
////}