package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.node.Node
import de.vulpescloud.node.logging.Logger
import de.vulpescloud.node.service.ServiceFactory
import de.vulpescloud.node.task.TaskProvider
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.IntegerParser
import org.incendo.cloud.parser.standard.StringParser

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
    }
}