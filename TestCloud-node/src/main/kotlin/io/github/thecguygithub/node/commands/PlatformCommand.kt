package io.github.thecguygithub.node.commands

import io.github.thecguygithub.api.command.CommandInfo
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.StringParser

class PlatformCommand {

    val logger = Logger()

    val platformService = Node.platformService!!

    val commandManager = Node.commandProvider?.commandManager!!

    init {
        Node.commandProvider?.registeredCommands?.add(
            CommandInfo(
                "platforms",
                setOf("platforms"),
                "Show information about the Platforms.",
                listOf("")
            )
        )

        commandManager.buildAndRegister("platforms", aliases = arrayOf("platforms")) {
            literal("list")

            handler { _ ->

                logger.info("Following &b${platformService.platforms.size} &7platforms are loaded&8:")
                platformService.platforms.forEach { platform ->
                    logger.info(
                        "&8- &f${platform!!.id}&8: (&7${platform.details()}&8)"
                    )
                }

            }
        }

        commandManager.buildAndRegister("platforms", aliases = arrayOf("platforms")) {
            literal("platforms")
            required("Platform-Name", StringParser.stringParser(StringParser.StringMode.SINGLE))



            handler { context ->

                val platform = Node.platformService!!.find(context.get<String>("Platform-Name").toString())

                if (platform != null) {
                    logger.info("All Version assigned to this Platform:")
                    logger.info(platform.versions.size)
                    val vers = platform.versions
                        .toList()

                    vers.forEach { it -> logger.info(it.version!!) }
                } else {
                    logger.warn("The Platform you have entered does not exist!")
                }
            }
        }

    }

}