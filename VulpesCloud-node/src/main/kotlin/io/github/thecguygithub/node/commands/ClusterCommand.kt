package io.github.thecguygithub.node.commands

import io.github.thecguygithub.api.command.CommandInfo
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.StringParser

class ClusterCommand {

    init {

        val logger = Logger()

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