package de.vulpescloud.node.commands

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotations.Description
import org.incendo.cloud.annotations.Command

@Suppress("Unused")
@Description("COMMANDS.DESCRIPTION.cluster")
class ClusterCommand(private val clusterProvider: ClusterProvider) {

    @Command("cluster list")
    fun displayClusterInfo(source: CommandSource) {
        val nodes = clusterProvider.nodes().sortedBy { it.name }
        val maxNodeNameLength = nodes.maxOfOrNull { it.name.length } ?: 0
        source.sendMessage("Showing &m${nodes.size} &7node(s)&8:")
        nodes.forEach {
            val paddedName = it.name.padEnd(maxNodeNameLength)
            source.sendMessage(
                " &8- &m$paddedName &7State&8: &e${it.state.name}&8, &7Version&8: &e${it.cloudVersion.fullVersion}&8, &7HeadNode&8: &e${it.headNode}"
            )
        }
    }
}
