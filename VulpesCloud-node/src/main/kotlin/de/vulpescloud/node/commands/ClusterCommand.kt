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

class ClusterCommand {

    init {

        val logger = LoggerFactory.getLogger(ClusterCommand::class.java)

        Node.commandProvider?.registeredCommands?.add(
            CommandInfo(
                "cluster",
                setOf("clu"),
                "Manage the Cluster.",
                listOf("cluster")
            )
        )

        val cm = Node.commandProvider!!.commandManager!!
        val cluProv = Node.clusterProvider

        cm.buildAndRegister("cluster", aliases = arrayOf("clu")) {
            literal("nodes")
            literal("list")
            flag("uuid")

            handler { ctx ->

                if (ctx.flags().isPresent("uuid")) {

                    logger.info("The Following ${cluProv.nodes.size} are registered:")
                    cluProv.nodes.forEach { logger.info(" - ${it.name} (${it.uuid})") }

                } else {
                    logger.info("The Following ${cluProv.nodes.size} are registered:")
                    cluProv.nodes.forEach { logger.info(" - ${it.name}") }
                }
            }
        }

        cm.buildAndRegister("cluster", aliases = arrayOf("clu")) {
            literal("node")
            required("node", StringParser.stringParser(StringParser.StringMode.SINGLE))
            flag("uuid")

            handler { ctx ->

                if (ctx.flags().isPresent("uuid")) {

                    logger.info("The Following ${cluProv.nodes.size} are registered:")
                    cluProv.nodes.forEach { logger.info(" - ${it.name} (${it.uuid})") }

                } else {
                    logger.info("The Following ${cluProv.nodes.size} are registered:")
                    cluProv.nodes.forEach { logger.info(" - ${it.name}") }
                }
            }
        }
    }

}