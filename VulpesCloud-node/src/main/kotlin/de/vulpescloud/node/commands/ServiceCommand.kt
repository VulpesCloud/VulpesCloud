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
import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.api.services.ServiceMessageBuilder
import de.vulpescloud.api.services.action.ServiceActions
import de.vulpescloud.node.Node
import de.vulpescloud.node.service.Service
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
                                RedisPubSubChannels.VULPESCLOUD_SERVICE_ACTION.name
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
                            RedisPubSubChannels.VULPESCLOUD_SERVICE_ACTION.name
                        )
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
            literal("screen")

            handler { ctx ->
                val service = Node.serviceProvider.findByName(ctx.get("service"))

                if (service != null) {
                    val srv = service as Service
                    if (srv.logging) {
                        srv.logging = false
                        logger.info("Service logging has been disabled!")
                    } else {
                        srv.logging = true
                        logger.info("Service logging has been enabled!")
                    }
                } else {
                    logger.info("The entered Service does not exist!")
                }
            }
        }
    }
}