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
import org.incendo.cloud.parser.standard.StringParser
import org.slf4j.LoggerFactory

class VersionCommand {

    val logger: org.slf4j.Logger = LoggerFactory.getLogger(VersionCommand::class.java)

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