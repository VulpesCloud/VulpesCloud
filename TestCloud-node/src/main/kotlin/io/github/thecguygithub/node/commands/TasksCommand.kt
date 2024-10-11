package io.github.thecguygithub.node.commands

import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.terminal.setup.impl.TaskSetup
import kotlin.Unit


//class TasksCommand : Command("group", "Manage or create your cluster groups", "groups") {
//
//    val logger = Logger()
//
//    init {
//        val groupService = Node.taskProvider
//
//        // argument for group name
//        val groupArgument = CommandArgumentType.ClusterTask("task")
//
//        syntax  ({ context ->
//            logger.info("Following &b${groupService?.groups()?.size} &7groups are loaded&8:")
//            groupService?.groups()?.forEach { group -> logger.info("&8- &f${group.name()}&8: (&7${group.details()}&8)") }
//        }, "List all registered groups&8.", CommandArgumentType.Keyword("list"))
//
//        syntax({ context -> TaskSetup().run() }, "Create a new cluster group.", CommandArgumentType.Keyword("create"))
//
//        syntax({ context ->
//            groupService?.delete(context.arg(groupArgument))
//                ?.ifPresentOrElse(
//                    { s -> logger.warn("Cannot delete group: $s") },
//                    { logger.info("Successfully delete group ${context.arg(groupArgument)} in cluster!") }
//                )
//        }, "Delete the selected group&8.", groupArgument, CommandArgumentType.Keyword("delete"))

//        syntax({ context ->
//            val group = context.arg(groupArgument)
//            logger.info("Name&8: &b{}", group.name())
//            logger.info("Runtime nodes&8: &b{}", String.join("&8, &b", group.nodes()))
//            logger.info("Platform&8: &b{}", group.platform().details())
//            logger.info("Static service&8: &b{}", group.staticService())
//            logger.info("Maximum memory&8: &b{}mb", group.maxMemory())
//            logger.info("Minimum online services&8: &b{}", group.minOnlineServerInstances())
//            logger.info("Maximum online services&8: &b{}", group.maxOnlineServerInstances())
//            logger.info("Maximal players: &b{}", group.maxPlayers())
//            logger.info("Properties&8(&b{}&8):", group.properties().pool().size())
//        }, "Show all information about a group&8.", groupArgument, Keyword("info"))
//
//        syntax({ context ->
//            val group: Unit = context.arg(groupArgument)
//            if (group.services().isEmpty()) {
//                log.info("This group has no services&8!")
//                return@syntax
//            }
//
//            log.info("Stopping all services of the group &8'&f{}&8'...", group.name())
//            for (service in group.services()) {
//                service.shutdown()
//            }
//        }, "Shutdown all services with this group&8.", groupArgument, Keyword("shutdown"))
//
//
//        val editKey = Enum<E>(
//            ClusterGroupEditFields::class.java, "key"
//        )
//        val editValue = Text("value")
//
//        syntax({ context ->
//            val group: Unit = context.arg(groupArgument)
//            val editableField: Unit = context.arg(editKey)
//            val value: Unit = context.arg(editValue)
//            log.info("{}:{}: {}", group.name(), editableField.name(), value)
//        }, "Change a property of a group&8.", groupArgument, Keyword("edit"), editKey, editValue)
//    }
//}