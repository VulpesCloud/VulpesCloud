package io.github.thecguygithub.node.commands

import io.github.thecguygithub.api.command.CommandInfo
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import org.incendo.cloud.kotlin.extension.buildAndRegister

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
    }
}