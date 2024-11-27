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