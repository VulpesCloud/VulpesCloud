package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.virtualconfig.v1.getAllRequest
import de.vulpescloud.api.virtualconfig.VirtualConfig
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

@Suppress("UNUSED")
class VirtualConfigCommand {

    @Suggestions("virtualConfig")
    fun suggestConfigs(): Stream<String> {
        return CompletableFuture.supplyAsync {
                runBlocking {
                    Node.instance.localGrpcClient.virtualConfigAPI
                        .getAll(getAllRequest {})
                        .configsList
                }
            }
            .get(5, TimeUnit.SECONDS)
            .stream()
            .map { it.name }
    }

    @Parser(suggestions = "virtualConfig")
    fun virtualConfigParser(input: CommandInput): VirtualConfig {
        val name = input.readString()

        return CompletableFuture.supplyAsync {
                runBlocking {
                    Node.instance.localGrpcClient.virtualConfigAPI
                        .getAll(getAllRequest {})
                        .configsList
                        .find { it.name == name }
                        ?.let { VirtualConfig.fromDefinition(it) }
                        ?: throw IllegalStateException(
                            "Could not find virtual config with name $name"
                        )
                }
            }
            .get(5, TimeUnit.SECONDS)
    }

    @Command("virtualconfigs list")
    fun listVirtualConfigs(source: CommandSource, @Flag("local") local: Boolean) {
        NodeCoroutineScope.launch {
            if (local) {
                source.sendMessage("Listing local virtual configs...")
                Node.instance.virtualConfigProvider.apply {
                    tempConfigsPath
                        .toFile()
                        .listFiles()
                        ?.filter { it.extension == "json" }
                        ?.forEach { file -> source.sendMessage(" - ${file.nameWithoutExtension}") }
                }
            } else {
                source.sendMessage("Listing remote virtual configs...")
                Node.instance.localGrpcClient.virtualConfigAPI
                    .getAll(getAllRequest {})
                    .configsList
                    .forEach { source.sendMessage(" - ${it.name}") }
            }
        }
    }

    @Command("virtualconfigs config <config> show")
    fun showVirtualConfig(
        source: CommandSource,
        @Flag("force") force: Boolean,
        @Argument("config") config: VirtualConfig,
    ) {
        NodeCoroutineScope.launch {
            Node.instance.virtualConfigProvider.getCustomConfig(config.name, force).let {
                if (it == null) {
                    source.sendMessage(
                        "Unexpected NullPointerException while trying to get the config! Try with --force"
                    )
                    return@let
                }

                source.sendMessage("Name: ${it.name}")
                source.sendMessage("Created at: ${formatUnixTimestamp(it.createdAt)}")
                source.sendMessage("Updated at: ${formatUnixTimestamp(it.lastUpdatedAt)}")
                source.sendMessage("Raw Json: ${it.config.toString(4)}")
            }
        }
    }

    @Command("virtualconfigs config <config> updateFromLocal")
    fun updateVirtualConfigFromLocal(
        source: CommandSource,
        @Argument("config") config: VirtualConfig,
    ) {
        NodeCoroutineScope.launch {
            Node.instance.virtualConfigProvider.updateDatabaseFromLocalConfig(config.name)
            source.sendMessage(
                "Successfully updated the database from the local config file for virtual config ${config.name}!"
            )
        }
    }

    @Command("virtualconfigs config <config> updateFromDatabase")
    fun updateVirtualConfigFromDatabase(
        source: CommandSource,
        @Argument("config") config: VirtualConfig,
    ) {
        NodeCoroutineScope.launch {
            Node.instance.virtualConfigProvider.updateLocalConfigFromDatabase(config.name)
            source.sendMessage(
                "Successfully updated the local config file from the database for virtual config ${config.name}!"
            )
        }
    }

    fun formatUnixTimestamp(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)

        val formatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())

        return formatter.format(instant)
    }
}
