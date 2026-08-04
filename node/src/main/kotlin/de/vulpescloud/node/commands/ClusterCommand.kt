package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.cluster.v2.getAllNodesRequest
import build.buf.gen.vulpescloud.cluster.v2.getNodeSnapshotRequest
import build.buf.gen.vulpescloud.cluster.v2.snapshotOrNull
import de.vulpescloud.api.cluster.NodeEndpointDetails
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.CommandSource
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput

class ClusterCommand {

    @Suggestions("nodes")
    fun suggestNodes(): Stream<String> {
        return CompletableFuture.supplyAsync {
                runBlocking {
                    Node.Companion.instance.localGrpcClient.clusterAPI
                        .getAllNodes(getAllNodesRequest {})
                        .nodesList
                        .map { it.name }
                        .stream()
                }
            }
            .get(5, TimeUnit.SECONDS)
    }

    @Parser(suggestions = "nodes")
    fun parseNodes(input: CommandInput): List<NodeEndpointDetails> {
        return CompletableFuture.supplyAsync {
                runBlocking {
                    val regexPattern = input.readString().replace("*", ".*")
                    val regex = Regex(regexPattern)

                    Node.instance.localGrpcClient.clusterAPI
                        .getAllNodes(getAllNodesRequest {})
                        .nodesList
                        .map { NodeEndpointDetails.Companion.fromDefinition(it) }
                        .filter { it.name.matches(regex) }
                }
            }
            .thenApply { it }
            .exceptionally { throw it }
            .get(5, TimeUnit.SECONDS)
    }

    @Permission("cluster.getAll")
    @Command("cluster nodes")
    fun listNodes(source: CommandSource) {
        runBlocking {
            source.sendMessage("<gray>Listing all nodes...</gray>")
            try {
                Node.Companion.instance.localGrpcClient.clusterAPI
                    .getAllNodes(getAllNodesRequest {})
                    .nodesList
                    .forEach { node ->
                        val state =
                            Node.instance.clusterProvider.remoteNodes
                                .find { it.endpoint.name == node.name }
                                ?.channel
                                ?.getState(true)

                        source.sendMessage(
                            " <dark_gray>»</dark_gray> <white>${node.name}</white> <dark_gray>| <gray>State:</gray> <white></white> <dark_gray>| <gray>Connection:</gray> <white>$state</white>"
                        )
                    }
            } catch (exception: Exception) {
                source.sendMessage("<red>An error occurred while trying to get the nodes!</red>")
                exception.printStackTrace()
                return@runBlocking
            }
        }
    }

    @Permission("cluster.getSnapshot")
    @Command("cluster node <node> getSnapshot")
    fun getNodeSnapshot(
        source: CommandSource,
        @Argument("node") endpoints: List<NodeEndpointDetails>,
    ) {
        runBlocking {
            endpoints.forEach { endpoint ->
                source.sendMessage(
                    "<gray>Fetching snapshot for node</gray> <white>${endpoint.name}</white><gray>...</gray>"
                )
                val remoteNode =
                    Node.instance.clusterProvider.remoteNodes.find { n ->
                        n.endpoint.name == endpoint.name
                    }

                Node.instance.localGrpcClient.clusterAPI
                    .getNodeSnapshot(getNodeSnapshotRequest { this.name = endpoint.name })
                    .snapshotOrNull
                    ?.let { snapshot ->
                        source.sendMessage(
                            "<gold>---------</gold> <white>${snapshot.name}</white> <gold>---------</gold>"
                        )
                        source.sendMessage(
                            "<gray>CPU Usage<dark_gray>:</dark_gray> <white>${snapshot.systemCpuUsage}%</white>"
                        )
                        source.sendMessage(
                            "<gray>Services Memory Usage<dark_gray>:</dark_gray> <white>${snapshot.servicesUsedMemory}mb</white>"
                        )
                        source.sendMessage(
                            "<gray>Services Max Memory Usage<dark_gray>:</dark_gray> <white>${snapshot.servicesMaxMemory}mb</white>"
                        )
                        source.sendMessage(
                            "<gray>State<dark_gray>:</dark_gray> <white>${snapshot.state}</white>"
                        )
                        source.sendMessage(
                            "<gray>Online Players<dark_gray>:</dark_gray> <white>${snapshot.onlinePlayers}</white>"
                        )
                        source.sendMessage(
                            "<gray>Connection State<dark_gray>:</dark_gray> <white>${remoteNode?.channel?.getState(true)}</white>"
                        )
                        return@let
                    }
                    ?: source.sendMessage(
                        "<red>Failed to fetch snapshot for node ${endpoint.name}!</red>"
                    )
            }
        }
    }
}
