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

import build.buf.gen.vulpescloud.services.v1.*
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Default
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.processors.confirmation.annotation.Confirmation
import org.vulpesstudios.vulpescloud.api.services.Service
import org.vulpesstudios.vulpescloud.api.services.ServiceStates
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.command.CommandSource
import org.vulpesstudios.vulpescloud.node.command.ConsoleCommandSource
import org.vulpesstudios.vulpescloud.node.command.ScopedCommandPermissions
import org.vulpesstudios.vulpescloud.node.command.annotation.Alias
import org.vulpesstudios.vulpescloud.node.command.annotation.SpecificCommandSource
import org.vulpesstudios.vulpescloud.node.services.ServiceLogHandler
import org.vulpesstudios.vulpescloud.node.utils.TableUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.text.Regex
import kotlin.text.RegexOption
import kotlin.text.equals
import kotlin.text.isNotEmpty
import kotlin.text.lastIndexOf
import kotlin.text.lowercase
import kotlin.text.split
import kotlin.text.substring
import kotlin.text.trim
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Alias(["ser"])
class ServiceCommand {

    private val allColumns = listOf(
        "name",
        "taskName",
        "orderedId",
        "port",
        "node",
        "playerCount",
        "startTime",
        "state",
        "hostname"
    )

    private val defaultColumns = listOf(
        "name",
        "port",
        "state",
        "playerCount",
        "node",
        "startTime"
    )

    private val startTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    @Suggestions("service")
    fun serviceSuggestions(): Stream<String> {
        return ServiceCache.getTasks().map { Service.fromDefinition(it).name() }.stream()
    }

    @Parser(suggestions = "service")
    fun serviceParser(input: CommandInput): List<Service> {
        val raw = input.readString()
        val pattern = raw.split("*").joinToString(".*") { Regex.escape(it) }
        val regex = Regex("^$pattern$", RegexOption.IGNORE_CASE)

        return ServiceCache.getTasks()
            .map { Service.fromDefinition(it) }
            .filter { regex.matches(it.name()) }
    }

    @Suggestions("service-columns")
    fun serviceColumnSuggestions(
        ctx: CommandContext<CommandSource>,
        input: CommandInput,
    ): List<String> {
        val current = input.lastRemainingToken()
        val prefix = current.substring(0, current.lastIndexOf(',') + 1)
        val selected = prefix.split(',')
            .filter { it.isNotEmpty() }
            .map { it.lowercase() }
            .toSet()

        if ("all" in selected || "default" in selected) return emptyList()

        val candidates = if (selected.isEmpty()) allColumns + listOf("all", "default") else allColumns

        return candidates
            .filter { it.lowercase() !in selected }
            .map { prefix + it }
    }

    @Permission("services.getAll")
    @Command("services|ser list [columns]")
    fun listServices(
        source: CommandSource,
        @Argument("columns", suggestions = "service-columns") @Default("default") columns: String,
        @Flag("all") all: Boolean,
    ) {
        val selected: List<String> =
            when {
                all || columns.equals("all", ignoreCase = true) -> allColumns
                columns.equals("default", ignoreCase = true) -> defaultColumns
                else -> {
                    val requested = columns.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    val invalid = requested.filter { r ->
                        allColumns.none { it.equals(r, ignoreCase = true) }
                    }

                    if (invalid.isNotEmpty()) {
                        source.sendMessage("Unknown column(s): ${invalid.joinToString()}")
                        source.sendMessage("Available: ${allColumns.joinToString()}")
                        return
                    }

                    requested
                        .map { r -> allColumns.first { it.equals(r, ignoreCase = true) } }
                        .distinct()
                }
            }

        runBlocking {
            val services = Node.instance.localGrpcClient.serviceAPI
                .getAllServices(GetAllServicesRequest.newBuilder().build())
                .servicesList

            val table = TableUtils.newTable().defaultStyle()

            table.header(
                mapOf(
                    "name" to "Name",
                    "taskName" to "Task",
                    "orderedId" to "Ordered ID",
                    "port" to "Port",
                    "node" to "Node",
                    "playerCount" to "Players",
                    "startTime" to "Start Time",
                    "state" to "State",
                    "hostname" to "Hostname",
                )
            )

            services.forEach { service ->
                table.addContent(
                    mapOf(
                        "name" to "${service.task.name}-${service.orderedId}",
                        "taskName" to service.task.name,
                        "orderedId" to service.orderedId.toString(),
                        "port" to service.port.toString(),
                        "node" to service.node,
                        "playerCount" to service.playerCount.toString(),
                        "startTime" to startTimeFormatter.format(
                            Instant.ofEpochSecond(service.startTime.seconds, service.startTime.nanos.toLong())
                        ),
                        "state" to service.state.name,
                        "hostname" to service.hostname,
                    )
                )
            }

            table.lines(selected).forEach { line ->
                source.sendMessage(line)
            }
        }
    }

    @Permission("services.get")
    @Command("services|service <service> info")
    fun infoService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            var metadataMessage = ""
            it.metadata.forEach { (key, value) ->
                metadataMessage += "    - $key: $value\n"
            }

            source.sendMessage(
                "<gold>---------</gold> <white>${it.name()}</white> <gold>---------</gold>\n" +
                    "<gray>UUID<dark_gray>:</dark_gray> <white>${it.uuid}</white>\n" +
                    "<gray>Port<dark_gray>:</dark_gray> <white>${it.port}</white>\n" +
                    "<gray>Node<dark_gray>:</dark_gray> <white>${it.node}</white>\n" +
                    "<gray>Players<dark_gray>:</dark_gray> <white>${it.playerCount}</white>\n" +
                    "<gray>State<dark_gray>:</dark_gray> <white>${it.state}</white>\n" +
                    "<gray>StartTime<dark_gray>:</dark_gray> <white>${it.startTime}</white>\n" +
                    "<gray>MetaData<dark_gray>:</dark_gray> <white>:</white>\n" +
                    metadataMessage
            )
        }
    }

    @Permission("services.start")
    @Command("services|ser <service> start")
    fun startService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            if (!runBlocking { ScopedCommandPermissions.hasPermission(source, "services.start", mapOf("task" to setOf(it.task.name), "node" to setOf(it.node), "service" to setOf(it.name()))) }) {
                source.sendError("You don't have permission to start ${it.name()}.")
                return@forEach
            }
            if (it.state == ServiceStates.STARTING || it.state == ServiceStates.RUNNING) {
                source.sendMessage("<red>Service is already running!</red>")
                return
            }
            runBlocking {
                source.sendMessage(
                    "<gray>Starting service</gray> <white>${it.task.name}-${it.orderedId}</white><gray>...</gray>"
                )
                Node.instance.localGrpcClient.serviceAPI.startService(
                    StartServiceRequest.newBuilder().setService(it.toDefinition()).build()
                )
            }
        }
    }

    @Permission("services.stop")
    @Command("services|ser <service> stop")
    fun stopService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            if (!runBlocking { ScopedCommandPermissions.hasPermission(source, "services.stop", mapOf("task" to setOf(it.task.name), "node" to setOf(it.node), "service" to setOf(it.name()))) }) {
                source.sendError("You don't have permission to stop ${it.name()}.")
                return@forEach
            }
            runBlocking {
                Node.instance.localGrpcClient.serviceAPI.stopService(
                    StopServiceRequest.newBuilder().setService(it.toDefinition()).build()
                )
            }
        }
    }

    @Permission("services.restart")
    @Command("services|ser <service> restart")
    fun restartService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            if (!runBlocking { ScopedCommandPermissions.hasPermission(source, "services.restart", mapOf("task" to setOf(it.task.name), "node" to setOf(it.node), "service" to setOf(it.name()))) }) {
                source.sendError("You don't have permission to restart ${it.name()}.")
                return@forEach
            }
            runBlocking {
                val response = Node.instance.localGrpcClient.serviceAPI.restartService(
                    RestartServiceRequest.newBuilder().setService(it.toDefinition()).build()
                )
                if (response.success) source.sendMessage("<green>Restarted ${it.name()}.</green>")
                else source.sendError("Failed to restart ${it.name()}: ${response.error}")
            }
        }
    }

    @Permission("services.getSnapshot")
    @Command("services|ser <service> snapshot")
    fun getSnapshot(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            if (!runBlocking { ScopedCommandPermissions.hasPermission(source, "services.getSnapshot", mapOf("task" to setOf(it.task.name), "node" to setOf(it.node), "service" to setOf(it.name()))) }) {
                source.sendError("You don't have permission to view the snapshot for ${it.name()}.")
                return@forEach
            }
            runBlocking {
                source.sendMessage(
                    "<gray>Retrieving snapshot for service</gray> <white>${it.task.name}-${it.orderedId}</white><gray>...</gray>"
                )
                val snapshot =
                    Node.instance.localGrpcClient.serviceAPI
                        .getLatestServiceSnapshot(
                            getLatestServiceSnapshotRequest { this.service = it.toDefinition() }
                        )
                        .snapshot
                source.sendMessage(
                    "<gold>---------</gold> <white>${it.task.name}-${it.orderedId}</white> <gold>---------</gold>\n" +
                        "<gray>Timestamp<dark_gray>:</dark_gray> <white>${snapshot.snapshotTime}</white>\n" +
                        "<gray>Players<dark_gray>:</dark_gray> <white>${snapshot.playerCount}</white>\n" +
                        "<gray>System CPU<dark_gray>:</dark_gray> <white>${snapshot.systemCpuUsage}%</white>\n" +
                        "<gray>Process CPU<dark_gray>:</dark_gray> <white>${snapshot.cpuUsage}%</white>\n" +
                        "<gray>Current Memory Usage<dark_gray>:</dark_gray> <white>${snapshot.memoryUsageBytes}mb</white>\n" + //TODO: Format correctly
                        "<gray>Uptime<dark_gray>:</dark_gray> <white>${snapshot.uptimeMillis.toDuration(DurationUnit.MILLISECONDS)}</white>"
                )
            }
        }
    }

    @Permission("services.delete")
    @Command("services|ser <service> delete")
    fun deleteService(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            if (!runBlocking { ScopedCommandPermissions.hasPermission(source, "services.delete", mapOf("task" to setOf(it.task.name), "node" to setOf(it.node), "service" to setOf(it.name()))) }) {
                source.sendError("You don't have permission to delete ${it.name()}.")
                return@forEach
            }
            runBlocking {
                Node.instance.localGrpcClient.serviceAPI.deleteService(
                    DeleteServiceRequest.newBuilder().setService(it.toDefinition()).build()
                )
            }
        }
    }

    @Permission("services.stopAll")
    @Confirmation
    @Command("services|ser stopAll")
    fun stopAllService(source: CommandSource) {
        runBlocking {
            source.sendMessage("<gray>Stopping all services...</gray>")
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(GetAllServicesRequest.newBuilder().build())
                .servicesList
                .forEach {
                    Node.instance.localGrpcClient.serviceAPI.stopService(
                        StopServiceRequest.newBuilder().setService(it).build()
                    )
                }
        }
    }

    @Permission("services.deleteAll")
    @Confirmation
    @Command("services|ser deleteAll")
    fun deleteAllServices(source: CommandSource) {
        runBlocking {
            source.sendMessage("<gray>Deleting all services...</gray>")
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(GetAllServicesRequest.newBuilder().build())
                .servicesList
                .forEach {
                    Node.instance.localGrpcClient.serviceAPI.deleteService(
                        DeleteServiceRequest.newBuilder().setService(it).build()
                    )
                }
        }
    }

    @Permission("services.sendCommand")
    @Command("services|ser <service> command <command>")
    fun sendCommand(
        source: CommandSource,
        @Argument("service") service: List<Service>,
        @Greedy @Argument("command") command: String,
    ) {
        runBlocking {
            service.forEach {
                if (!ScopedCommandPermissions.hasPermission(source, "services.sendCommand", mapOf("task" to setOf(it.task.name), "node" to setOf(it.node), "service" to setOf(it.name())))) {
                    source.sendError("You don't have permission to send commands to ${it.name()}.")
                    return@forEach
                }
                val resp =
                    Node.instance.localGrpcClient.serviceAPI.sendCommand(
                        build.buf.gen.vulpescloud.services.v1.sendCommandRequest {
                            this.service = it.toDefinition()
                            this.command = command
                        }
                    )
                if (resp.success) {
                    source.sendMessage(
                        "<gray>Sent command to service</gray> <white>${it.task.name}-${it.orderedId}</white><dark_gray>.</dark_gray>"
                    )
                } else {
                    source.sendMessage(
                        "<red>Failed to send command to service</red> <white>${it.task.name}-${it.orderedId}</white><dark_gray>.</dark_gray>"
                    )
                }
            }
        }
    }

    @SpecificCommandSource(ConsoleCommandSource::class)
    @Command("services|ser <service> screen")
    fun toggleScreen(source: CommandSource, @Argument("service") service: List<Service>) {
        service.forEach {
            ServiceLogHandler.toggleServiceLogging("${it.task.name}-${it.orderedId}")
        }
    }
}

object ServiceCache {
    private val cache =
        Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build<String, List<ServiceDefinition>>()

    fun getTasks(): List<ServiceDefinition> {
        return cache.get("tasks") {
            runBlocking {
                Node.instance.localGrpcClient.serviceAPI
                    .getAllServices(getAllServicesRequest {})
                    .servicesList
            }
        }
    }
}
