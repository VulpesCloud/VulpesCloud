/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.commands

import build.buf.gen.vulpescloud.virtualconfig.v1.getAllRequest
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.vulpesstudios.vulpescloud.api.virtualconfig.VirtualConfig
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.command.CommandSource
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

    @Permission("virtualconfig.list")
    @Command("virtualconfigs list")
    fun listVirtualConfigs(source: CommandSource, @Flag("local") local: Boolean) {
        runBlocking {
            if (local) {
                source.sendMessage("<gray>Listing local virtual configs...</gray>")
                Node.instance.virtualConfigProvider.apply {
                    tempConfigsPath
                        .toFile()
                        .listFiles()
                        ?.filter { it.extension == "json" }
                        ?.forEach { file -> source.sendMessage(" <dark_gray>»</dark_gray> <white>${file.nameWithoutExtension}</white>") }
                }
            } else {
                source.sendMessage("<gray>Listing remote virtual configs...</gray>")
                Node.instance.localGrpcClient.virtualConfigAPI
                    .getAll(getAllRequest {})
                    .configsList
                    .forEach { source.sendMessage(" <dark_gray>»</dark_gray> <white>${it.name}</white>") }
            }
        }
    }

    @Permission("virtualconfig.get")
    @Command("virtualconfigs config <config> show")
    fun showVirtualConfig(
        source: CommandSource,
        @Flag("force") force: Boolean,
        @Argument("config") config: VirtualConfig,
    ) {
        runBlocking {
            Node.instance.virtualConfigProvider.getCustomConfig(config.name, force).let {
                if (it == null) {
                    source.sendMessage(
                        "<red>Unexpected NullPointerException while trying to get the config! Try with --force</red>"
                    )
                    return@let
                }

                source.sendMessage("<gold>---------</gold> <white>${it.name}</white> <gold>---------</gold>")
                source.sendMessage("<gray>Created at<dark_gray>:</dark_gray> <white>${formatUnixTimestamp(it.createdAt)}</white>")
                source.sendMessage("<gray>Updated at<dark_gray>:</dark_gray> <white>${formatUnixTimestamp(it.lastUpdatedAt)}</white>")
                source.sendMessage("<gray>Raw Json<dark_gray>:</dark_gray> <white>${it.config.toString(4)}</white>")
            }
        }
    }

    @Permission("virtualconfig.updateFromLocal")
    @Command("virtualconfigs config <config> updateFromLocal")
    fun updateVirtualConfigFromLocal(
        source: CommandSource,
        @Argument("config") config: VirtualConfig,
    ) {
        runBlocking {
            Node.instance.virtualConfigProvider.updateDatabaseFromLocalConfig(config.name)
            source.sendMessage(
                "<green>Successfully updated the database from the local config file for virtual config</green> <white>${config.name}</white><green>!</green>"
            )
        }
    }

    @Permission("virtualconfig.updateFromDatabase")
    @Command("virtualconfigs config <config> updateFromDatabase")
    fun updateVirtualConfigFromDatabase(
        source: CommandSource,
        @Argument("config") config: VirtualConfig,
    ) {
        runBlocking {
            Node.instance.virtualConfigProvider.updateLocalConfigFromDatabase(config.name)
            source.sendMessage(
                "<green>Successfully updated the local config file from the database for virtual config</green> <white>${config.name}</white><green>!</green>"
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
