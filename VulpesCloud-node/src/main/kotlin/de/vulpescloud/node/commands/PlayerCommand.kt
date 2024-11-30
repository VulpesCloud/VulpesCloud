/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.node.Node
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.slf4j.LoggerFactory

class PlayerCommand {
    private val commandProvider = Node.commandProvider!!
    private val logger = LoggerFactory.getLogger(PlayerCommand::class.java)

    init {
        Node.commandProvider?.registeredCommands?.add(
            CommandInfo(
                "player",
                setOf("players"),
                "Manage the Players.",
                listOf("")
            )
        )

        commandProvider.commandManager!!.buildAndRegister("player", aliases = arrayOf("players")) {
            literal("online")
            literal("list")
            handler { _ ->
                logger.info("Current online players: ")
                Node.playerProvider.players()!!.forEach { logger.info(" - {} {}", it!!.name(), it.details()) }
            }
        }
        commandProvider.commandManager!!.buildAndRegister("player", aliases = arrayOf("players")) {
            literal("override")
            handler { _ ->
                logger.info("Overriding local Players!")
                Node.playerProvider.overridePlayersFromRedis()
            }
        }
    }

}