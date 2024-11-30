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
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.service.ServiceFactory
import de.vulpescloud.node.service.ServiceStartScheduler
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.IntegerParser
import org.incendo.cloud.parser.standard.StringParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class DevCommand {
    val logger: Logger = LoggerFactory.getLogger(DevCommand::class.java)

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
        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("debug")
            literal("serviceFactory")
            literal("isIdPresent")
            required("integer", IntegerParser.integerParser())

            handler { ctx ->
                logger.info(
                    ServiceFactory().isIdPresent(
                        Node.taskProvider.findByName("Test")!!,
                        ctx.get("integer")
                    ).toString()
                )
            }
        }
        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("debug")
            literal("serviceFactory")
            literal("generateOrderedId")

            handler { _ ->
                logger.info(
                    ServiceFactory().generateOrderedId(
                        Node.taskProvider.findByName("Test")!!
                    ).toString()
                )
            }
        }
        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("debug")
            literal("serviceFactory")
            literal("detectServicePort")

            handler { _ ->
                logger.info(
                    ServiceFactory().detectServicePort(
                        Node.taskProvider.findByName("Test")!!
                    ).toString()
                )
            }
        }

        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("sss")
            literal("start")

            handler { _ ->
                logger.info("Scheduling ServiceStartScheduler")
                ServiceStartScheduler().schedule()
            }
        }
        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("sss")
            literal("cancel")

            handler { _ ->
                logger.info("Canceling ServiceStartScheduler")
                ServiceStartScheduler.instance.cancel()
            }
        }

        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("services")

            handler { _ ->
                Node.taskProvider.tasks()!!.forEach {
                    logger.info(it.name() + ">>>>>>")
                    it.services()!!.forEach { ser ->
                        logger.info(it.name() + ">>>>>>" + ser!!.name())
                    }
                }
            }
        }

        Node.commandProvider!!.commandManager!!.buildAndRegister("dev") {
            literal("sss")
            literal("once")

            handler { _ ->
                CompletableFuture.runAsync {
                    for (task: Task in Node.taskProvider.tasks()!!) {
                        if (task.nodes().contains(Node.nodeConfig!!.name)) continue

                        if (task.minOnlineCount() < task.services()!!.size) continue

                        logger.warn(task.name())
                        logger.warn(task.minOnlineCount().toString())
                        logger.warn(task.services()!!.size.toString())

                        val serviceToStart = task.minOnlineCount() - task.services()!!.size

                        logger.warn(serviceToStart.toString())

                        for (i in 0 until serviceToStart) {
                            Node.serviceProvider.factory().startServiceOnTask(task)
                        }
                    }
                }
            }
        }
    }
}