package io.github.thecguygithub.node.commands

import io.github.thecguygithub.api.command.CommandInfo
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.StringParser

class ServiceCommand {
    val logger = Logger()

    init {
        Node.commandProvider?.registeredCommands?.add(
            CommandInfo(
                "service",
                setOf("ser", "services"),
                "Manage the Services.",
                listOf("")
            )
        )

        Node.commandProvider!!.commandManager!!.buildAndRegister("service", aliases = arrayOf("ser", "services")) {
            literal("list")

            handler { _ ->
                logger.info("The following &b${Node.serviceProvider.services()!!.size} &7services are loaded&8:")
                Node.serviceProvider.services()!!.forEach { service -> logger.info("&8- &f${service.name()} &8: (&7${service.details()}&8)") }
            }
        }

        Node.commandProvider!!.commandManager!!.buildAndRegister("service", aliases = arrayOf("ser", "services")) {
            required("service", StringParser.stringParser(StringParser.StringMode.SINGLE))
            literal("command")
            required("command", StringParser.stringParser(StringParser.StringMode.GREEDY))
            flag("force")

            handler { ctx ->
                val service = Node.serviceProvider.findByName(ctx.get("service"))

                if (service != null) {
                    try {
                        if (ctx.flags().isPresent("force")) {
                            service.executeCommand(ctx.get("command"))
                        } else {
                            Node.instance!!.getRC()?.sendMessage("SERVICE;${service.name()};ACTION;COMMAND;" + ctx.get("command"), "vulpescloud-action-service")
                        }
                    } catch (e: Exception) {
                        logger.error(e)
                    }
                } else {
                    logger.info("The entered Service does not exist!")
                }
            }
        }

        Node.commandProvider!!.commandManager!!.buildAndRegister("service", aliases = arrayOf("ser", "services")) {
            required("service", StringParser.stringParser(StringParser.StringMode.SINGLE))
            literal("stop")

            handler { ctx ->
                val service = Node.serviceProvider.findByName(ctx.get("service"))

                if (service != null) {
                    try {
                        Node.instance!!.getRC()?.sendMessage("SERVICE;${service.name()};ACTION;STOP", "vulpescloud-action-service")
                    } catch (e: Exception) {
                        logger.error(e)
                    }
                } else {
                    logger.info("The entered Service does not exist!")
                }
            }
        }
    }
}