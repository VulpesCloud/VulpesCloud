package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.node.Node
import de.vulpescloud.node.logging.Logger
import org.incendo.cloud.kotlin.extension.buildAndRegister
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
    }
}