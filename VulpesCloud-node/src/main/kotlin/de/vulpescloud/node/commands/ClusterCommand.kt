package de.vulpescloud.node.commands

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.node.command.CommandSource
import org.incendo.cloud.annotations.Command

@Suppress("Unused")
class ClusterCommand(
    private val clusterProvider: ClusterProvider
) {

    @Command("cluster list")
    fun displayClusterInfo(
        source: CommandSource
    ) {
        source.sendMessage("Showing &m${clusterProvider.nodes().size} &7nodes&8:")
        clusterProvider.nodes().forEach {
            source.sendMessage(" &8- &m${it.name} &7Version&8:<yellow>${it.cloudVersion} &7HeadNode&8:<yellow>${it.headNode}")
        }
    }

}
