package io.github.thecguygithub.node.commands
//
//import io.github.thecguygithub.api.command.CommandInfo
//import io.github.thecguygithub.node.Node
//import io.github.thecguygithub.node.logging.Logger
//import io.github.thecguygithub.node.platforms.Platform
//import io.github.thecguygithub.node.platforms.versions.PlatformVersion
//import io.github.thecguygithub.node.tasks.TaskJson
//import io.github.thecguygithub.node.terminal.setup.impl.TaskSetup
//import org.incendo.cloud.kotlin.extension.buildAndRegister
//import org.incendo.cloud.parser.standard.StringParser
//
//class TasksCommand {
//    val logger = Logger()
//
//    init {
//        Node.commandProvider?.registeredCommands?.add(
//            CommandInfo(
//                "task",
//                setOf("tasks"),
//                "Clear the Terminal.",
//                listOf("")
//            )
//        )
//
//        Node.commandProvider!!.commandManager!!.buildAndRegister("task", aliases = arrayOf("tasks")) {
//            literal("list")
//
//            handler { _ ->
//                logger.info("Following &b${Node.taskProvider?.groups()?.size} &7groups are loaded&8:")
//                Node.taskProvider?.groups()
//                    ?.forEach { group -> logger.info("&8- &f${group.name()}&8: (&7${group.details()}&8)") }
//            }
//        }
//
//        Node.commandProvider!!.commandManager!!.buildAndRegister("task", aliases = arrayOf("tasks")) {
//            literal("task")
//            required("task", StringParser.stringParser(StringParser.StringMode.SINGLE))
//            literal("start")
//
//            handler { ctx ->
//                val task = Node.taskProvider!!.find(ctx.get<String>("task").toString())
//
//                if (task != null) {
//                    logger.info("Staring a Service!")
//                    Node.serviceProvider!!.factory().runGroupService(task)
//
//                } else {
//                    logger.info("The Task that has been entered is not valid!")
//                }
//            }
//
//        }
//
//        Node.commandProvider!!.commandManager!!.buildAndRegister("task", aliases = arrayOf("tasks")) {
//            literal("create")
//            flag("default")
//
//            handler { ctx ->
//
//                logger.debug(ctx.flags().get("default")!!)
//
//                if (ctx.flags().isPresent("default")) {
//
//                    try {
//                        logger.debug("--default is present!")
//                        logger.debug("searching Platform")
//
//                        val platform = Node.platformService!!.find("paper")
//                        if (platform == null) {
//                            logger.error("The Platform is null!")
//                            return@handler
//                        }
//
//                        logger.debug("found platform, looking for version!")
//                        val version = platform.versions.stream().filter { it.version.equals("1.21.1", true)}.findFirst().orElse(null)
//
//                        if (version == null) {
//                            logger.error("version is empty!")
//                        }
//
//                        logger.debug("Version found! Creating json!")
//
//                        val json  = TaskJson.createGroupJson(
//                            "Test",
//                            platform,
//                            "1.21.1",
//                            512,
//                            false,
//                            1,
//                            false,
//                            false,
//                            25565
//                        )
//                        logger.debug("JSON created! sending redis message!")
//
//                        Node.instance!!.getRC()?.sendMessage(json.toString(), "testcloud-events-group-create")
//
//                        logger.debug("Redis message sent successfully!")
//                    } catch (e: Exception) {
//                        logger.error("HEWWE IS A  EWWOWRS" + e.toString())
//                    }
//                } else {
//                    // TODO("FIX TASK SETUP")
//                    TaskSetup().run()
//                    Logger().info("test")
//                }
//            }
//        }
//
//
//
//
//    }
//}
//
//
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