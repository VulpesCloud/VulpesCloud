package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.events.v1.nodeLockEvent
import build.buf.gen.vulpescloud.node.v1.ClusterAPIServiceGrpcKt
import build.buf.gen.vulpescloud.node.v1.getAllNodesRequest
import build.buf.gen.vulpescloud.node.v1.getNodeSnapshotRequest
import build.buf.gen.vulpescloud.node.v1.pingRequest
import build.buf.gen.vulpescloud.node.v1.snapshotOrNull
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeState
import de.vulpescloud.node.Node
import de.vulpescloud.node.cluster.ClusterHelper
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.event.EventsService
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.processors.confirmation.annotation.Confirmation

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

    @Permission("cluster.getAll")
    @Command("cluster nodes")
    fun listNodes(source: CommandSource) {
        runBlocking {
            source.sendMessage("<gray>Listing all nodes...</gray>")
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
                            " <dark_gray>»</dark_gray> <white>${node.name}</white> <dark_gray>| <gray>State:</gray> <white>${node.state}</white> <dark_gray>| <gray>Connection:</gray> <white>$state</white> <dark_gray>| <gray>Head:</gray> <white>${node.head}</white>"
                        )
                    }
            } catch (exception: Exception) {
                source.sendMessage("<red>An error occurred while trying to get the nodes!</red>")
                exception.printStackTrace()
                return@runBlocking
            }
        }
    }

    @Permission("cluster.node.ping")
    @Command("cluster node <node> ping")
    fun pingNode(source: CommandSource, @Argument("node") node: List<ClusterNode>) {
        runBlocking {
            node.forEach { clusterNode ->
                source.sendMessage(
                    "<gray>Pinging node</gray> <white>${clusterNode.name}</white><gray>...</gray>"
                )
                val remoteNode =
                    Node.instance.clusterProvider.remoteNodes.find { n ->
                        n.endpoint.name == clusterNode.name
                    }
                runCatching {
                        withTimeout(5.seconds) {
                            measureTime {
                                    val stub =
                                        ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineStub(
                                                remoteNode!!.channel!!
                                            )
                                            .withInterceptors(
                                                AuthClientInterceptor(Node.instance.secret)
                                            )
                                    stub.ping(pingRequest {})
                                }
                                .let { time ->
                                    source.sendMessage(
                                        "<gray>Pinged node</gray> <white>${clusterNode.name}</white> <gray>in</gray> <white>$time</white>"
                                    )
                                }
                        }
                    }
                    .onFailure {
                        source.sendMessage("<red>Failed to ping node due to: ${it.message}!</red>")
                    }
            }
        }
    }

    @Permission("cluster.node.lock")
    @Command("cluster node <node> lock")
    @Confirmation
    fun lockNode(source: CommandSource, @Argument("node") node: List<ClusterNode>) {
        runBlocking {
            node.forEach { clusterNode ->
                if (clusterNode.name == Node.instance.configProvider.config.nodeName) {
                    source.sendMessage("<red>A node cannot lock itself!</red>")
                    return@forEach
                }

                if (clusterNode.locked) {
                    source.sendMessage("<red>Node is already locked!</red>")
                    return@forEach
                }

                ClusterHelper.updateNode(clusterNode.copy(locked = true, state = NodeState.UNKNOWN))
                EventsService.publish(
                    nodeLockEvent {
                        this.node =
                            clusterNode
                                .copy(locked = true, state = NodeState.UNKNOWN)
                                .toDefinition()
                    }
                )
                source.sendMessage("<gray>Locked node</gray> <white>${clusterNode.name}</white>")
            }
        }
    }

    @Permission("cluster.node.unlock")
    @Command("cluster node <node> unlock")
    @Confirmation
    fun unlockNode(source: CommandSource, @Argument("node") node: List<ClusterNode>) {
        runBlocking {
            node.forEach { clusterNode ->
                if (clusterNode.name == Node.instance.configProvider.config.nodeName) {
                    source.sendMessage("<red>A node cannot unlock itself!</red>")
                    return@forEach
                }
                if (!clusterNode.locked) {
                    source.sendMessage("<red>Node is not locked!</red>")
                    return@forEach
                }
                ClusterHelper.updateNode(
                    clusterNode.copy(locked = false, state = NodeState.UNKNOWN)
                )
                EventsService.publish(
                    nodeLockEvent {
                        this.node =
                            clusterNode
                                .copy(locked = false, state = NodeState.UNKNOWN)
                                .toDefinition()
                    }
                )
                source.sendMessage("<gray>Unlocked node</gray> <white>${clusterNode.name}</white>")
            }
        }
    }

    @Permission("cluster.getSnapshot")
    @Command("cluster node <node> getSnapshot")
    fun getNodeSnapshot(source: CommandSource, @Argument("node") node: List<ClusterNode>) {
        runBlocking {
            node.forEach {
                source.sendMessage(
                    "<gray>Fetching snapshot for node</gray> <white>${it.name}</white><gray>...</gray>"
                )
                val remoteNode =
                    Node.Companion.instance.clusterProvider.remoteNodes.find { n ->
                        n.endpoint.name == it.name
                    }
                Node.Companion.instance.localGrpcClient.clusterAPI
                    .getNodeSnapshot(getNodeSnapshotRequest { this.name = it.name })
                    .snapshotOrNull
                    ?.let { snapshot ->
                        source.sendMessage(
                            "<gold>---------</gold> <white>${snapshot.name}</white> <gold>---------</gold>"
                        )
                        source.sendMessage(
                            "<gray>CPU Usage<dark_gray>:</dark_gray> <white>${snapshot.cpuUsage}%</white>"
                        )
                        source.sendMessage(
                            "<gray>Memory Usage<dark_gray>:</dark_gray> <white>${snapshot.usedMemory}mb</white>"
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
                        source.sendMessage(
                            "<gray>Head<dark_gray>:</dark_gray> <white>${it.head}</white>"
                        )
                        return@let
                    }
                    ?: source.sendMessage(
                        "<red>Failed to fetch snapshot for node ${it.name}!</red>"
                    )
            }
        }
    }
}
