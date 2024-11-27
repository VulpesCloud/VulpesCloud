package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.node.Node
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.slf4j.LoggerFactory

class ModuleCommand {
    private val commandProvider = Node.commandProvider!!
    private val logger = LoggerFactory.getLogger(ModuleCommand::class.java)

    init {
        Node.commandProvider?.registeredCommands?.add(
            CommandInfo(
                "module",
                setOf("modules"),
                "Manage the Modules.",
                listOf("")
            )
        )

        commandProvider.commandManager!!.buildAndRegister("module", aliases = arrayOf("modules")) {
            literal("loaded")
            handler { _ ->
                logger.info("All currently loaded Modules:")
                Node.moduleProvider.loadedModules().forEach {
                    logger.info(" - &m{} &8| &7 Authors: &b {} &7 Description: &b{}", it.moduleInfo.name, it.moduleInfo.author, it.moduleInfo.description)
                }
            }
        }
    }

}