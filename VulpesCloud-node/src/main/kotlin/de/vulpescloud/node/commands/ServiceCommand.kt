package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.api.services.ServiceMessageBuilder
import de.vulpescloud.api.services.action.ServiceActions
import de.vulpescloud.node.Node
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.StringParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ServiceCommand {
    val logger: Logger = LoggerFactory.getLogger(ServiceCommand::class.java)

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
                Node.serviceProvider.services()!!
                    .forEach { service -> logger.info("&8- &f${service.name()} &8: (&7${service.details()}&8)") }
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
                            Node.instance!!.getRC()?.sendMessage(
                                ServiceMessageBuilder.actionMessageBuilder()
                                    .setService(service)
                                    .setAction(ServiceActions.COMMAND)
                                    .setParameter(ctx.get("command"))
                                    .build(),
                                RedisPubSubChannels.VULPESCLOUD_ACTION_SERVICE.name
                            )
                        }
                    } catch (e: Exception) {
                        logger.error(e.toString())
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
                        Node.instance!!.getRC()?.sendMessage(
                            ServiceMessageBuilder.actionMessageBuilder()
                                .setService(service)
                                .setAction(ServiceActions.STOP)
                                .build(),
                            RedisPubSubChannels.VULPESCLOUD_ACTION_SERVICE.name
                        )
                    } catch (e: Exception) {
                        logger.error(e.toString())
                    }
                } else {
                    logger.info("The entered Service does not exist!")
                }
            }
        }
    }
}