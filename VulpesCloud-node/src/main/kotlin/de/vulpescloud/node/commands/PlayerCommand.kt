package de.vulpescloud.node.commands

import de.vulpescloud.node.Node
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.slf4j.LoggerFactory

class PlayerCommand {
    private val commandProvider = Node.commandProvider!!
    private val logger = LoggerFactory.getLogger(PlayerCommand::class.java)

    init {
        commandProvider.commandManager!!.buildAndRegister("player", aliases = arrayOf("players")) {
            literal("online")
            literal("list")
            handler { _ ->
                logger.info("Current online players: ")
                Node.playerProvider.players()!!.forEach { logger.info(" - {} {}", it!!.name(), it.details()) }
            }
        }
    }

}