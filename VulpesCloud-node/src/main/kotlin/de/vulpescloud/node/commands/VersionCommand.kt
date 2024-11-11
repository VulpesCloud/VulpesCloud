package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.node.Node
import de.vulpescloud.node.logging.Logger
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.StringParser

class VersionCommand {

    private val logger = Logger()

    init {
        Node.commandProvider?.registeredCommands?.add(
            CommandInfo(
                "version",
                setOf("versions"),
                "Get information about the Versions",
                listOf("version")
            )
        )

        val commandManager = Node.commandProvider!!.commandManager!!
        val versionProvider = Node.versionProvider

        commandManager.buildAndRegister("version", aliases = arrayOf("versions")) {
            literal("list")
            handler { _ ->
                logger.info("The Following ${versionProvider.versions.size} Versions are loaded:")
                versionProvider.versions.forEach { logger.info(" - ${it.name} (${it.type})") }
            }
        }

        commandManager.buildAndRegister("version", aliases = arrayOf("versions")) {
            literal("version")
            required("version", StringParser.stringParser(StringParser.StringMode.SINGLE))

            handler { ctx ->
                val version = versionProvider.search(ctx.get("version"))

                if (version != null) {
                    logger.info("The Following ${version.versions.size} Server Versions are loaded:")
                    version.versions.forEach { logger.info(" -  ${it.version}") }
                }
            }
        }
    }

}