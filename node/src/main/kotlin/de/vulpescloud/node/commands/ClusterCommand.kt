package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.node.v1.getAllNodesRequest
import build.buf.gen.vulpescloud.node.v1.getNodeSnapshotRequest
import build.buf.gen.vulpescloud.node.v1.snapshotOrNull
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

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
    fun parseNodes(input: CommandInput): List<ClusterNode> {
        return CompletableFuture.supplyAsync {
            runBlocking {
                val regexPattern = input.readString().replace("*", ".*")
                val regex = Regex(regexPattern)

                Node.Companion.instance.localGrpcClient.clusterAPI
                    .getAllNodes(getAllNodesRequest {})
                    .nodesList
                    .map { ClusterNode.Companion.fromDefinition(it) }
                    .filter { it.name.matches(regex) }
            }
            }
            .thenApply { it }
            .exceptionally { throw it }
            .get(5, TimeUnit.SECONDS)
    }

    @Command("cluster nodes")
    fun listNodes(source: CommandSource) {
        NodeCoroutineScope.launch {
            source.sendMessage("Listing all nodes...")
            try {
                Node.Companion.instance.localGrpcClient.clusterAPI
                    .getAllNodes(getAllNodesRequest {})
                    .nodesList
                    .forEach { node ->
                        val state =
                            Node.Companion.instance.clusterProvider.remoteNodes
                                .find { it.endpoint.name == node.name }
                                ?.channel
                                ?.getState(true)

                        source.sendMessage(
                            " - ${node.name} State: ${node.state} Connection State: $state Head: ${node.head}"
                        )
                    }
            } catch (exception: Exception) {
                source.sendMessage("An error occurred while trying to get the nodes!")
                exception.printStackTrace()
                return@launch
            }
        }
    }

    @Command("cluster node <node> getSnapshot")
    fun getNodeSnapshot(source: CommandSource, @Argument("node") node: List<ClusterNode>) {
        NodeCoroutineScope.launch {
            node.forEach {
                source.sendMessage("Fetching snapshot for node ${it.name}...")
                val remoteNode = Node.Companion.instance.clusterProvider.remoteNodes.find { n -> n.endpoint.name == it.name }
                Node.Companion.instance.localGrpcClient.clusterAPI
                    .getNodeSnapshot(getNodeSnapshotRequest { this.name = it.name })
                    .snapshotOrNull
                    ?.let { snapshot ->
                        source.sendMessage("--------- ${snapshot.name} ---------")
                        source.sendMessage("Cpu Usage: ${snapshot.cpuUsage}%")
                        source.sendMessage("Memory Usage: ${snapshot.usedMemory}mb")
                        source.sendMessage("State: ${snapshot.state}")
                        source.sendMessage("Online Players: ${snapshot.onlinePlayers}")
                        source.sendMessage("Connection State: ${remoteNode?.channel?.getState(true)}")
                        source.sendMessage("Head: ${it.head}")
                        return@let
                    } ?: source.sendMessage("Failed to fetch snapshot for node ${it.name}!")
            }
        }
    }
}