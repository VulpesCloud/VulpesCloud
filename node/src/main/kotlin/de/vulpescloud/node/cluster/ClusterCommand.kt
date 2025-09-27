package de.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.node.v1.getAllNodesRequest
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import kotlinx.coroutines.launch
import org.incendo.cloud.annotations.Command

class ClusterCommand {

    @Command("cluster nodes")
    fun listNodes(source: CommandSource) {
        NodeCoroutineScope.launch {
            source.sendMessage("Listing all nodes...")
            try {
                Node.instance.localGrpcClient.clusterAPI
                    .getAllNodes(getAllNodesRequest {})
                    .nodesList
                    .forEach { node ->
                        val state =
                            Node.instance.clusterProvider.remoteNodes
                                .find { it.endpoint.name == node.name }
                                ?.channel
                                ?.getState(true)

                        source.sendMessage(
                            " - ${node.name} State: ${node.state} Connection State: $state"
                        )
                    }
            } catch (exception: Exception) {
                source.sendMessage("An error occurred while trying to get the nodes!")
                exception.printStackTrace()
                return@launch
            }
        }
    }
}
