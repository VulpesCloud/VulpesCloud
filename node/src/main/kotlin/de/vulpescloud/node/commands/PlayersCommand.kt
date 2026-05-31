package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.node.v1.SoftwareType
import build.buf.gen.vulpescloud.players.v1.OnlinePlayer
import build.buf.gen.vulpescloud.players.v1.connectPlayerRequest
import build.buf.gen.vulpescloud.players.v1.getAllOnlinePlayersRequest
import build.buf.gen.vulpescloud.players.v1.getOfflinePlayersRequest
import build.buf.gen.vulpescloud.players.v1.kickPlayerRequest
import build.buf.gen.vulpescloud.players.v1.sendActionBarRequest
import build.buf.gen.vulpescloud.players.v1.sendMessageRequest
import build.buf.gen.vulpescloud.players.v1.sendTitleRequest
import build.buf.gen.vulpescloud.services.v1.ServiceState
import build.buf.gen.vulpescloud.services.v1.getAllServicesRequest
import de.vulpescloud.api.services.Service
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotation.Alias
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotation.specifier.Quoted
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput

@Alias(["players"])
class PlayersCommand {
    private val playerAPI by lazy { Node.instance.localGrpcClient.playerAPI }

    @Suggestions("onlinePlayers")
    fun suggestOnlinePlayers(): Stream<String> {
        return CompletableFuture.supplyAsync {
                runBlocking {
                    Node.instance.localGrpcClient.playerAPI
                        .getAllOnlinePlayers(getAllOnlinePlayersRequest {})
                        .onlinePlayersList
                }
            }
            .get(5, TimeUnit.SECONDS)
            .stream()
            .map { it.name }
    }

    @Parser(suggestions = "onlinePlayers")
    fun parseOnlinePlayers(input: CommandInput): List<OnlinePlayer> {
        val raw = input.readString()
        val pattern = raw.split("*").joinToString(".*") { Regex.escape(it) }
        val regex = Regex("^$pattern$", RegexOption.IGNORE_CASE)

        return CompletableFuture.supplyAsync {
                runBlocking {
                    Node.instance.localGrpcClient.playerAPI
                        .getAllOnlinePlayers(getAllOnlinePlayersRequest {})
                        .onlinePlayersList
                        .filter { regex.matches(it.name) }
                }
            }
            .get(5, TimeUnit.SECONDS)
    }

    @Permission("players.getAllOffline")
    @Command("player|players registered list")
    fun listRegisteredPlayers(source: CommandSource, @Flag("v") verbose: Boolean) {
        runBlocking {
            val registeredPlayers =
                playerAPI.getAllOfflinePlayers(getOfflinePlayersRequest {}).offlinePlayersList
            source.sendMessage(
                "<gray>A total of</gray> <gold>${registeredPlayers.size}</gold> <gray>players are registered!</gray>"
            )
            if (verbose) {
                registeredPlayers.forEach { player ->
                    source.sendMessage(
                        " <dark_gray>»</dark_gray> <white>${player.name}</white> <dark_gray>(</dark_gray><gray>${player.uuid}</gray><dark_gray>)</dark_gray> <gray>lastSeen:</gray> <white>${player.lastSeen}</white> <gray>firstSeen:</gray> <white>${player.firstSeen}</white>"
                    )
                }
            }
        }
    }

    @Permission("players.getAllOnline")
    @Command("player|players online list")
    fun listOnlinePlayers(source: CommandSource, @Flag("v") verbose: Boolean) {
        runBlocking {
            val onlinePlayers =
                playerAPI.getAllOnlinePlayers(getAllOnlinePlayersRequest {}).onlinePlayersList

            source.sendMessage(
                "<gray>A total of</gray> <gold>${onlinePlayers.size}</gold> <gray>players are online!</gray>"
            )
            if (verbose) {
                onlinePlayers.forEach { player ->
                    source.sendMessage(
                        " <dark_gray>»</dark_gray> <white>${player.name}</white> <dark_gray>(</dark_gray><gray>${player.uuid}</gray><dark_gray>)</dark_gray> <gray>Proxy:</gray> <white>${player.proxyServiceName}</white> <gray>Server:</gray> <white>${player.serverServiceName}</white>"
                    )
                }
            }
        }
    }

    @Command("player|players online message <onlinePlayer> <message>")
    fun sendMessage(
        source: CommandSource,
        @Argument("onlinePlayer") onlinePlayers: List<OnlinePlayer>,
        @Greedy @Argument("message") message: String,
    ) {
        runBlocking {
            onlinePlayers.forEach { player ->
                Node.instance.localGrpcClient.playerActionsAPI.sendMessage(
                    sendMessageRequest {
                        this.uuid = player.uuid
                        this.message = message
                    }
                )
                source.sendMessage("<gray>Sent message to</gray> <white>${player.name}</white>")
            }
        }
    }

    @Suggestions("onlineServers")
    fun suggestOnlineServers(): Stream<String> {
        return CompletableFuture.supplyAsync {
                runBlocking {
                    Node.instance.localGrpcClient.serviceAPI
                        .getAllServices(getAllServicesRequest {})
                        .servicesList
                        .filter {
                            it.state == ServiceState.SERVICE_STATE_RUNNING &&
                                it.task.serverSoftware.type == SoftwareType.SOFTWARE_TYPE_SERVER
                        }
                }
            }
            .get(5, TimeUnit.SECONDS)
            .stream()
            .map { "${it.task.name}-${it.orderedId}" }
    }

    @Parser(suggestions = "onlineServers")
    fun parseOnlineServers(input: CommandInput): Service {
        val raw = input.readString()
        val pattern = raw.split("*").joinToString(".*") { Regex.escape(it) }
        val regex = Regex("^$pattern$", RegexOption.IGNORE_CASE)

        val services =
            CompletableFuture.supplyAsync {
                    runBlocking {
                        Node.instance.localGrpcClient.serviceAPI
                            .getAllServices(getAllServicesRequest {})
                            .servicesList
                            .filter {
                                it.state == ServiceState.SERVICE_STATE_RUNNING &&
                                    it.task.serverSoftware.type == SoftwareType.SOFTWARE_TYPE_SERVER
                            }
                            .filter { regex.matches("${it.task.name}-${it.orderedId}") }
                    }
                }
                .get(5, TimeUnit.SECONDS)

        return Service.fromDefinition(services.first())
    }

    @Command("player|players online kick <onlinePlayer> [reason]")
    fun kickPlayer(
        source: CommandSource,
        @Argument("onlinePlayer") onlinePlayers: List<OnlinePlayer>,
        @Greedy @Argument("reason") reason: String?,
    ) {
        runBlocking {
            onlinePlayers.forEach { player ->
                Node.instance.localGrpcClient.playerActionsAPI.kickPlayer(
                    kickPlayerRequest {
                        this.uuid = player.uuid
                        this.reason = reason ?: "Kicked by an operator"
                    }
                )
                source.sendMessage("<gray>Kicked</gray> <white>${player.name}</white>")
            }
        }
    }

    @Command("player|players online title <onlinePlayer> <title> <subtitle>")
    fun sendTitle(
        source: CommandSource,
        @Argument("onlinePlayer") onlinePlayers: List<OnlinePlayer>,
        @Quoted @Argument("title") title: String,
        @Quoted @Argument("subtitle") subtitle: String,
    ) {
        runBlocking {
            onlinePlayers.forEach { player ->
                Node.instance.localGrpcClient.playerActionsAPI.sendTitle(
                    sendTitleRequest {
                        this.uuid = player.uuid
                        this.title = title
                        this.subtitle = subtitle
                    }
                )
                source.sendMessage("<gray>Sent title to</gray> <white>${player.name}</white>")
            }
        }
    }

    @Command("player|players online actionbar <onlinePlayer> <message>")
    fun sendActionBar(
        source: CommandSource,
        @Argument("onlinePlayer") onlinePlayers: List<OnlinePlayer>,
        @Greedy @Argument("message") message: String,
    ) {
        runBlocking {
            onlinePlayers.forEach { player ->
                Node.instance.localGrpcClient.playerActionsAPI.sendActionBar(
                    sendActionBarRequest {
                        this.uuid = player.uuid
                        this.message = message
                    }
                )
                source.sendMessage("<gray>Sent action bar to</gray> <white>${player.name}</white>")
            }
        }
    }

    @Command("player|players online connect <onlinePlayer> <server>")
    fun connectPlayer(
        source: CommandSource,
        @Argument("onlinePlayer") onlinePlayers: List<OnlinePlayer>,
        @Argument("server") server: Service,
    ) {
        runBlocking {
            onlinePlayers.forEach { player ->
                Node.instance.localGrpcClient.playerActionsAPI.connectPlayer(
                    connectPlayerRequest {
                        this.uuid = player.uuid
                        this.targetServer = server.name()
                    }
                )
                source.sendMessage(
                    "<gray>Connected</gray> <white>${player.name}</white> <gray>to</gray> <white>${server.name()}</white>"
                )
            }
        }
    }
}
