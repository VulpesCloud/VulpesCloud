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

import build.buf.gen.vulpescloud.rollout.v1.*
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.command.CommandSource
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import com.google.protobuf.Duration as ProtoDuration

@Suppress("UNUSED")
class RolloutCommand {

    @Suggestions("rolloutStrategies")
    fun strategySuggestions(): Stream<String> = Stream.of("rolling", "passive", "immediate")

    @Suggestions("rolloutTargets")
    fun rolloutTargetSuggestions(): Stream<String> {
        val taskNames = TaskCache.getTasks().map { it.name }
        val rolloutIds = RolloutCache.getActiveRollouts().map { it.rolloutId }
        return (taskNames + rolloutIds).stream()
    }

    @Suggestions("activeRolloutIds")
    fun activeRolloutIdSuggestions(): Stream<String> =
        RolloutCache.getActiveRollouts().map { it.rolloutId }.stream()

    @Permission("rollout.restart")
    @Command("rollout restart <tasks>")
    fun restartRollout(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Flag(value = "strategy", suggestions = "rolloutStrategies") strategy: String?,
        @Flag(value = "batch", aliases = ["b"]) batch: Int?,
        @Flag(value = "drain-threshold") drainThreshold: Int?,
        @Flag(value = "drain-timeout", aliases = ["t"]) drainTimeout: Int?,
        @Flag(value = "readiness-timeout") readinessTimeout: Int?,
        @Flag(value = "bypass-max-services", aliases = ["force"]) bypassMaxServices: Boolean,
        @Flag(value = "no-transfer") noTransfer: Boolean,
    ) {
        val options =
            try {
                buildOptions(
                    strategy,
                    batch,
                    drainThreshold,
                    drainTimeout,
                    readinessTimeout,
                    bypassMaxServices,
                    noTransfer,
                )
            } catch (e: IllegalArgumentException) {
                source.sendMessage("<red>${e.message}</red>")
                return
            }

        runBlocking {
            tasks.forEach { task ->
                val response =
                    Node.instance.localGrpcClient.rolloutAPI.startRollout(
                        startRolloutRequest {
                            this.taskName = task.name
                            this.options = options
                        }
                    )

                if (!response.success) {
                    source.sendMessage(
                        "<red>Failed to start rollout for task</red> <white>${task.name}</white><red>:</red> <white>${response.error}</white>"
                    )
                    return@forEach
                }

                val rollout = response.rollout
                source.sendMessage(
                    "<green>Started rollout</green> <gold>${rollout.rolloutId}</gold> <gray>for task</gray> <white>${task.name}</white> " +
                        "<dark_gray>|</dark_gray> <gray>strategy:</gray> <white>${rollout.options.strategy.display()}</white> " +
                        "<dark_gray>|</dark_gray> <gray>services to replace:</gray> <white>${rollout.totalServicesToReplace}</white>"
                )
            }
        }
    }

    @Permission("rollout.view")
    @Command("rollout status <target>")
    fun rolloutStatus(
        source: CommandSource,
        @Argument(value = "target", suggestions = "rolloutTargets") target: String,
    ) {
        runBlocking {
            val isKnownTask = TaskCache.getTasks().any { it.name == target }

            val response =
                try {
                    Node.instance.localGrpcClient.rolloutAPI.getRolloutStatus(
                        getRolloutStatusRequest {
                            if (isKnownTask) this.taskName = target else this.rolloutId = target
                        }
                    )
                } catch (_: Exception) {
                    source.sendMessage(
                        "<red>No rollout found for</red> <white>$target</white><red>.</red>"
                    )
                    return@runBlocking
                }

            printRolloutStatus(source, response.rollout)
        }
    }

    @Permission("rollout.cancel")
    @Command("rollout cancel <rolloutId>")
    fun cancelRollout(
        source: CommandSource,
        @Argument(value = "rolloutId", suggestions = "activeRolloutIds") rolloutId: String,
    ) {
        runBlocking {
            val response =
                Node.instance.localGrpcClient.rolloutAPI.cancelRollout(
                    cancelRolloutRequest { this.rolloutId = rolloutId }
                )

            if (response.success) {
                source.sendMessage("<green>${response.message}</green>")
            } else {
                source.sendMessage("<red>${response.message}</red>")
            }
        }
    }

    @Permission("rollout.view")
    @Command("rollout list")
    fun listRollouts(source: CommandSource) {
        runBlocking {
            val rollouts =
                Node.instance.localGrpcClient.rolloutAPI
                    .listActiveRollouts(listActiveRolloutsRequest {})
                    .rolloutsList

            if (rollouts.isEmpty()) {
                source.sendMessage("<gray>There are no active rollouts.</gray>")
                return@runBlocking
            }

            source.sendMessage(
                "<gray>The following</gray> <gold>${rollouts.size}</gold> <gray>rollout(s) are active:</gray>"
            )
            rollouts.forEach {
                source.sendMessage(
                    " <dark_gray>»</dark_gray> <gold>${it.rolloutId}</gold> <dark_gray>|</dark_gray> <white>${it.taskName}</white> " +
                        "<dark_gray>|</dark_gray> <gray>status:</gray> ${statusColor(it.status)}${it.status.display()}</${statusColorName(it.status)}> " +
                        "<dark_gray>|</dark_gray> <gray>strategy:</gray> <white>${it.options.strategy.display()}</white> " +
                        "<dark_gray>|</dark_gray> <gray>batch:</gray> <white>${it.currentBatchNumber}/${it.totalBatches}</white> " +
                        "<dark_gray>|</dark_gray> <gray>replaced:</gray> <white>${it.servicesStopped}/${it.totalServicesToReplace}</white>"
                )
            }

            source.sendMessage(
                "<gray>Note: only currently active rollouts are shown; recently finished rollouts aren't exposed via the CLI yet.</gray>"
            )
        }
    }

    private fun printRolloutStatus(source: CommandSource, rollout: RolloutProgress) {
        source.sendMessage(
            "<gold>---------</gold> <white>${rollout.rolloutId}</white> <gold>---------</gold>\n" +
                "<gray>Task<dark_gray>:</dark_gray> <white>${rollout.taskName}</white> \n" +
                "<gray>Status<dark_gray>:</dark_gray> ${statusColor(rollout.status)}${rollout.status.display()}</${statusColorName(rollout.status)}> \n" +
                "<gray>Strategy<dark_gray>:</dark_gray> <white>${rollout.options.strategy.display()}</white> \n" +
                "<gray>Batch<dark_gray>:</dark_gray> <white>${rollout.currentBatchNumber}/${rollout.totalBatches}</white> \n" +
                "<gray>Services to replace<dark_gray>:</dark_gray> <white>${rollout.totalServicesToReplace}</white> \n" +
                "<gray>Started<dark_gray>:</dark_gray> <white>${rollout.servicesStarted}</white> <gray>| Ready:</gray> <white>${rollout.servicesReady}</white> <gray>| Stopped:</gray> <white>${rollout.servicesStopped}</white> \n" +
                "<gray>Remaining old services<dark_gray>:</dark_gray> <white>${rollout.pendingStopServiceNamesList.size}</white>" +
                if (rollout.failureReason.isNotBlank())
                    "\n<gray>Failure reason<dark_gray>:</dark_gray> <red>${rollout.failureReason}</red>"
                else ""
        )
    }

    private fun statusColor(status: RolloutStatus): String =
        when (status) {
            RolloutStatus.ROLLOUT_STATUS_COMPLETED -> "<green>"
            RolloutStatus.ROLLOUT_STATUS_FAILED -> "<red>"
            RolloutStatus.ROLLOUT_STATUS_CANCELLED -> "<red>"
            RolloutStatus.ROLLOUT_STATUS_DRAINING -> "<gold>"
            else -> "<yellow>"
        }

    private fun statusColorName(status: RolloutStatus): String =
        when (status) {
            RolloutStatus.ROLLOUT_STATUS_COMPLETED -> "green"
            RolloutStatus.ROLLOUT_STATUS_FAILED -> "red"
            RolloutStatus.ROLLOUT_STATUS_CANCELLED -> "red"
            RolloutStatus.ROLLOUT_STATUS_DRAINING -> "gold"
            else -> "yellow"
        }

    private fun RolloutStatus.display(): String = name.removePrefix("ROLLOUT_STATUS_")

    private fun RolloutStrategy.display(): String = name.removePrefix("ROLLOUT_STRATEGY_")

    private fun buildOptions(
        strategy: String?,
        batchSize: Int?,
        drainThreshold: Int?,
        drainTimeoutSeconds: Int?,
        readinessTimeoutSeconds: Int?,
        bypassMaxServices: Boolean,
        noTransfer: Boolean,
    ): RolloutOptions {
        return rolloutOptions {
            strategy?.let {
                this.strategy =
                    when (it.lowercase()) {
                        "rolling",
                        "rolling_replace",
                        "rolling-replace" -> RolloutStrategy.ROLLOUT_STRATEGY_ROLLING_REPLACE
                        "passive",
                        "passive_drain",
                        "passive-drain" -> RolloutStrategy.ROLLOUT_STRATEGY_PASSIVE_DRAIN
                        "immediate" -> RolloutStrategy.ROLLOUT_STRATEGY_IMMEDIATE
                        else ->
                            throw IllegalArgumentException(
                                "Unknown strategy '$it' (expected rolling, passive, or immediate)"
                            )
                    }
            }
            batchSize?.let { this.batchSize = it }
            drainThreshold?.let { this.drainPlayerThreshold = it }
            drainTimeoutSeconds?.let {
                this.drainTimeout = ProtoDuration.newBuilder().setSeconds(it.toLong()).build()
            }
            readinessTimeoutSeconds?.let {
                this.readinessTimeout = ProtoDuration.newBuilder().setSeconds(it.toLong()).build()
            }
            if (bypassMaxServices) this.bypassMaxServiceCount = true
            if (noTransfer) this.fallbackRemainingPlayers = false
        }
    }
}

object RolloutCache {
    private val cache =
        Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build<String, List<RolloutProgress>>()

    fun getActiveRollouts(): List<RolloutProgress> {
        return cache.get("active") {
            runBlocking {
                Node.instance.localGrpcClient.rolloutAPI
                    .listActiveRollouts(listActiveRolloutsRequest {})
                    .rolloutsList
            }
        }
    }
}
