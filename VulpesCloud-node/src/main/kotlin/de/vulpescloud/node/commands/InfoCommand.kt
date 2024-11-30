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
import org.incendo.cloud.description.Description
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InfoCommand {
    init {
        val logger: Logger = LoggerFactory.getLogger(InfoCommand::class.java)
        Node.commandProvider?.registeredCommands?.add(
            CommandInfo(
                "info",
                setOf("me"),
                "Gives information about the Node.",
                listOf("info | me")
            )
        )

        Node.commandProvider?.commandManager!!.buildAndRegister(
            "info", Description.of("Gives information about the Node."), aliases = arrayOf("me")) {
            handler { _ ->
                logger.info("Java version&8: &f${System.getProperty("java.version")}")
                logger.info("Operating System&8: &f${System.getProperty("os.name")}")
                logger.info("Used Memory of the Node process&8: &f${usedMemory()}")
                logger.info(" ")
            }
        }
    }

    private fun usedMemory(): String {
        return ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
            .freeMemory()) / 1024 / 1024).toString() + " mb"
    }

}