package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.logging.Logger
import de.vulpescloud.node.service.ServiceFactory
import de.vulpescloud.node.service.ServiceStartScheduler
import de.vulpescloud.node.task.TaskProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.IntegerParser
import org.incendo.cloud.parser.standard.StringParser
import java.util.concurrent.CompletableFuture

class DevCommand {
    val logger = Logger()

    init {
        Node.commandProvider?.registeredCommands?.add(
            CommandInfo(
                "dev",
                setOf(),
                "Development",
                listOf("")
            )
        )

        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("redis")
            literal("send")
            required("channel", StringParser.stringParser(StringParser.StringMode.SINGLE))
            required("message", StringParser.stringParser(StringParser.StringMode.SINGLE))

            handler { ctx ->
                Node.instance!!.getRC()?.sendMessage(ctx.get("message"), ctx.get("channel"))
            }
        }
        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("debug")
            literal("serviceFactory")
            literal("isIdPresent")
            required("integer", IntegerParser.integerParser())

            handler { ctx ->
                logger.info(ServiceFactory().isIdPresent(
                    Node.taskProvider.findByName("Test")!!,
                    ctx.get("integer")
                ))
            }
        }
        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("debug")
            literal("serviceFactory")
            literal("generateOrderedId")

            handler { _ ->
                logger.info(ServiceFactory().generateOrderedId(
                    Node.taskProvider.findByName("Test")!!
                ))
            }
        }
        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("debug")
            literal("serviceFactory")
            literal("detectServicePort")

            handler { _ ->
                logger.info(ServiceFactory().detectServicePort(
                    Node.taskProvider.findByName("Test")!!
                ))
            }
        }

        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("sss")
            literal("start")

            handler { _ ->
                logger.info("Scheduling ServiceStartScheduler")
                ServiceStartScheduler().schedule()
            }
        }
        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("sss")
            literal("cancel")

            handler { _ ->
                logger.info("Canceling ServiceStartScheduler")
                ServiceStartScheduler.instance.cancel()
            }
        }

        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("services")

            handler { _ ->
                Node.taskProvider.tasks()!!.forEach {
                    logger.info(it.name() + ">>>>>>")
                    it.services()!!.forEach { ser ->
                        logger.info(it.name() + ">>>>>>" + ser!!.name())
                    }
                }
            }
        }

        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("sss")
            literal("once")

            handler { _ ->
                CompletableFuture.runAsync {
                    for (task: Task in Node.taskProvider.tasks()!!) {
                        if (task.nodes().contains(Node.nodeConfig!!.name)) continue

                        if (task.minOnlineCount() < task.services()!!.size) continue

                        logger.warn(task.name())
                        logger.warn(task.minOnlineCount().toString())
                        logger.warn(task.services()!!.size.toString())

                        val serviceToStart = task.minOnlineCount() - task.services()!!.size

                        logger.warn(serviceToStart.toString())

                        for (i in 0 until serviceToStart) {
                            Node.serviceProvider.factory().startServiceOnTask(task)
                        }
                    }
                }
            }
        }
    }
}