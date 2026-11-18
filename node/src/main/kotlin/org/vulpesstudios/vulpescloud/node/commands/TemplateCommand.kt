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

import build.buf.gen.vulpescloud.templates.v1.*
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.command.CommandSource
import org.vulpesstudios.vulpescloud.node.templates.TemplateRegistry
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

class TemplateCommand {

    @Suggestions("templateNames")
    fun templateNameSuggestions(): Stream<String> =
        runBlocking { TemplateRegistry.getAll().map { it.name }.stream() }

    @Suggestions("activeTemplateJobIds")
    fun activeTemplateJobIdSuggestions(): Stream<String> =
        TemplateJobCache.getActiveJobs().map { it.jobId }.stream()

    @Suggestions("templateJobTargets")
    fun templateJobTargetSuggestions(): Stream<String> {
        val templateNames = runBlocking { TemplateRegistry.getAll().map { it.name } }
        val jobIds = TemplateJobCache.getActiveJobs().map { it.jobId }
        return (templateNames + jobIds).stream()
    }

    @Suggestions("templateConflictModes")
    fun conflictModeSuggestions(): Stream<String> =
        Stream.of("deny", "skip", "overwrite", "overwrite_newer", "overwrite_changed")

    @Suggestions("templateExtraFilesModes")
    fun extraFilesModeSuggestions(): Stream<String> =
        Stream.of("keep", "purge")

    @Permission("templates.list")
    @Command("template list")
    fun listTemplates(source: CommandSource) {
        runBlocking {
            val templates = TemplateRegistry.getAll()
            source.sendMessage("The following ${templates.size} template(s) are registered:")
            templates.forEach { template ->
                val additional = if (!template.location.nodeName.isNullOrBlank()) "-${template.location.nodeName}" else ""
                source.sendMessage(" - ${template.name}@${template.location.storageName}$additional | Weight: ${template.weight} | Enabled: ${template.enabled}")
            }
        }
    }

    @Permission("templates.listRegisteredStorages")
    @Command("template storages")
    fun listStorages(source: CommandSource) {
        runBlocking {
            val storages = Node.instance.localGrpcClient.templateAPI.listRegisteredStorages(
                listRegisteredStoragesRequest {  }).storageList
            source.sendMessage("The following ${storages.size} storages(s) are registered:")
            storages.forEach { storage ->
                val node = if (storage.nodeName != null) "@${storage.nodeName}" else ""
                source.sendMessage(" - Type: ${storage.type.name} | Name:${storage.name}$node")
            }
        }
    }

    @Permission("templates.copy")
    @Command("template copy <source> <destination>")
    fun copyTemplate(
        source: CommandSource,
        @Argument("source") sourceName: String,
        @Argument("destination") destinationName: String,
        @Flag(value = "conflict", suggestions = "templateConflictModes") conflictMode: String?,
        @Flag(value = "extra-files", suggestions = "templateExtraFilesModes") extraFilesMode: String?,
    ) {
        val conflict = conflictMode?.let { parseConflictMode(it, source) } ?: TemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_SKIP
        val extra = extraFilesMode?.let { parseExtraFilesMode(it, source) } ?: TemplateJobExtraFilesMode.TEMPLATE_JOB_EXTRA_FILES_MODE_KEEP

        runBlocking {
            val sourceTemplate = TemplateRegistry.getAll().find { it.name == sourceName }
                ?: run { source.sendMessage("<red>Template not found: $sourceName</red>"); return@runBlocking }

            val request = copyTemplateRequest {
                this.source = templateReference {
                    templateId = sourceTemplate.id
                    name = sourceTemplate.name
                    location = sourceTemplate.location.toDefinition()
                }
                this.destination = templateReference {
                    name = destinationName
                }
                this.conflictMode = conflict
                this.extraFilesMode = extra
            }

            val response = Node.instance.localGrpcClient.templateAPI.copyTemplate(request)
            val acceptStatus = response.acceptResult.status

            if (acceptStatus == TemplateJobAcceptStatus.TEMPLATE_JOB_ACCEPT_STATUS_ACCEPTED) {
                val job = response.acceptResult.job
                source.sendMessage(
                    "<green>Template copy started</green> <gold>${job.jobId}</gold> " +
                        "<gray>from</gray> <white>$sourceName</white> <gray>to</gray> <white>$destinationName</white>"
                )
            } else {
                source.sendMessage(
                    "<red>Template copy rejected</red>: <white>${response.acceptResult.reason}</white>"
                )
            }
        }
    }

    @Permission("templates.move")
    @Command("template move <source> <destination>")
    fun moveTemplate(
        source: CommandSource,
        @Argument("source") sourceName: String,
        @Argument("destination") destinationName: String,
        @Flag(value = "conflict", suggestions = "templateConflictModes") conflictMode: String?,
        @Flag(value = "extra-files", suggestions = "templateExtraFilesModes") extraFilesMode: String?,
    ) {
        val conflict = conflictMode?.let { parseConflictMode(it, source) } ?: TemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_SKIP
        val extra = extraFilesMode?.let { parseExtraFilesMode(it, source) } ?: TemplateJobExtraFilesMode.TEMPLATE_JOB_EXTRA_FILES_MODE_KEEP

        runBlocking {
            val sourceTemplate = TemplateRegistry.getAll().find { it.name == sourceName }
                ?: run { source.sendMessage("<red>Template not found: $sourceName</red>"); return@runBlocking }

            val request = moveTemplateRequest {
                this.source = templateReference {
                    templateId = sourceTemplate.id
                    name = sourceTemplate.name
                    location = sourceTemplate.location.toDefinition()
                }
                this.destination = templateReference {
                    name = destinationName
                }
                this.conflictMode = conflict
                this.extraFilesMode = extra
            }

            val response = Node.instance.localGrpcClient.templateAPI.moveTemplate(request)
            val acceptStatus = response.acceptResult.status

            if (acceptStatus == TemplateJobAcceptStatus.TEMPLATE_JOB_ACCEPT_STATUS_ACCEPTED) {
                val job = response.acceptResult.job
                source.sendMessage(
                    "<green>Template move started</green> <gold>${job.jobId}</gold> " +
                        "<gray>from</gray> <white>$sourceName</white> <gray>to</gray> <white>$destinationName</white>"
                )
            } else {
                source.sendMessage(
                    "<red>Template move rejected</red>: <white>${response.acceptResult.reason}</white>"
                )
            }
        }
    }

    @Permission("templates.job.status")
    @Command("template job status <target>")
    fun templateJobStatus(
        source: CommandSource,
        @Argument(value = "target", suggestions = "templateJobTargets") target: String,
    ) {
        runBlocking {
            val job = findJobByTarget(target, source)
                ?: return@runBlocking

            printJobStatus(source, job)
        }
    }

    @Permission("templates.job.list")
    @Command("template job list")
    fun listTemplateJobs(source: CommandSource) {
        runBlocking {
            val jobs = Node.instance.localGrpcClient.templateAPI
                .listActiveTemplateJobs(com.google.protobuf.Empty.getDefaultInstance())
                .jobsList

            if (jobs.isEmpty()) {
                source.sendMessage("<gray>There are no active template jobs.</gray>")
                return@runBlocking
            }

            source.sendMessage(
                "<gray>The following</gray> <gold>${jobs.size}</gold> <gray>template job(s) are active:</gray>"
            )
            jobs.forEach {
                source.sendMessage(
                    " <dark_gray>»</dark_gray> <gold>${it.jobId}</gold> <dark_gray>|</dark_gray> " +
                        "<white>${it.source.name}</white> <gray>→</gray> <white>${it.destination.name}</white> " +
                        "<dark_gray>|</dark_gray> <gray>status:</gray> ${jobStatusColor(it.status)}${it.status.name.removePrefix("TEMPLATE_JOB_STATUS_")}</${jobStatusColorName(it.status)}> " +
                        "<dark_gray>|</dark_gray> <gray>files:</gray> <white>${it.filesProcessed}/${it.totalFiles}</white>"
                )
            }
        }
    }

    @Permission("templates.job.cancel")
    @Command("template job cancel <jobId>")
    fun cancelTemplateJob(
        source: CommandSource,
        @Argument(value = "jobId", suggestions = "activeTemplateJobIds") jobId: String,
    ) {
        runBlocking {
            val response = Node.instance.localGrpcClient.templateAPI.cancelTemplateJob(
                cancelTemplateJobRequest { this.jobId = jobId }
            )

            if (response.success) {
                source.sendMessage("<green>${response.message}</green>")
            } else {
                source.sendMessage("<red>${response.message}</red>")
            }
        }
    }

    @Permission("templates.job.cancel")
    @Command("template job unlock <jobId>")
    fun unlockTemplateJob(
        source: CommandSource,
        @Argument("jobId") jobId: String,
    ) {
        runBlocking {
            val unlocked = Node.instance.templateServiceImpl.unlockTemplateJobManually(jobId)
            if (unlocked) {
                source.sendMessage("<green>Manual recovery acknowledged; template locks released for job $jobId</green>")
            } else {
                source.sendMessage("<red>Job $jobId is active, missing, or has no manual lock</red>")
            }
        }
    }

    private fun parseConflictMode(value: String, source: CommandSource): TemplateJobConflictMode? =
        try {
            when (value.lowercase()) {
                "deny" -> TemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_DENY
                "skip" -> TemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_SKIP
                "overwrite" -> TemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_OVERWRITE
                "overwrite_newer", "overwrite-newer" -> TemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_OVERWRITE_NEWER
                "overwrite_changed", "overwrite-changed" -> TemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_OVERWRITE_CHANGED
                else -> throw IllegalArgumentException("Unknown conflict mode '$value'")
            }
        } catch (e: IllegalArgumentException) {
            source.sendMessage("<red>${e.message}</red>")
            null
        }

    private fun parseExtraFilesMode(value: String, source: CommandSource): TemplateJobExtraFilesMode? =
        try {
            when (value.lowercase()) {
                "keep" -> TemplateJobExtraFilesMode.TEMPLATE_JOB_EXTRA_FILES_MODE_KEEP
                "purge" -> TemplateJobExtraFilesMode.TEMPLATE_JOB_EXTRA_FILES_MODE_PURGE
                else -> throw IllegalArgumentException("Unknown extra-files mode '$value'")
            }
        } catch (e: IllegalArgumentException) {
            source.sendMessage("<red>${e.message}</red>")
            null
        }

    private suspend fun findJobByTarget(target: String, source: CommandSource): TemplateJobProgress? {
        // Job IDs remain queryable after completion so users can inspect terminal results.
        runCatching {
            Node.instance.localGrpcClient.templateAPI.getTemplateJob(
                getTemplateJobRequest { jobId = target }
            )
        }.getOrNull()?.let { return it }

        // Otherwise treat it as a template name and search active jobs.
        val jobs = Node.instance.localGrpcClient.templateAPI
            .listActiveTemplateJobs(com.google.protobuf.Empty.getDefaultInstance())
            .jobsList

        // Otherwise treat as template name — find a job involving this template
        jobs.find { it.source.name == target || it.destination.name == target }?.let { return it }

        source.sendMessage("<red>No active job found for</red> <white>$target</white><red>.</red>")
        return null
    }

    private fun printJobStatus(source: CommandSource, job: TemplateJobProgress) {
        val typeLabel = when (job.type) {
            TemplateJobType.TEMPLATE_JOB_TYPE_COPY -> "Copy"
            TemplateJobType.TEMPLATE_JOB_TYPE_MOVE -> "Move"
            else -> "Unknown"
        }

        val failureLine =
            if (job.failureReason.isNotBlank())
                "\n<gray>Failure reason<dark_gray>:</dark_gray> <red>${job.failureReason}</red>"
            else ""

        source.sendMessage(
            "<gold>---------</gold> <white>${job.jobId}</white> <gold>---------</gold>\n" +
                "<gray>Type<dark_gray>:</dark_gray> <white>$typeLabel</white> \n" +
                "<gray>Status<dark_gray>:</dark_gray> ${jobStatusColor(job.status)}${job.status.name.removePrefix("TEMPLATE_JOB_STATUS_")}</${jobStatusColorName(job.status)}> \n" +
                "<gray>Source<dark_gray>:</dark_gray> <white>${job.source.name}</white> " +
                "<gray>@</gray> <white>${job.source.location.storageName}</white> \n" +
                "<gray>Destination<dark_gray>:</dark_gray> <white>${job.destination.name}</white> " +
                "<gray>@</gray> <white>${job.destination.location.storageName}</white> \n" +
                "<gray>Conflict mode<dark_gray>:</dark_gray> <white>${job.conflictMode.name.removePrefix("TEMPLATE_JOB_CONFLICT_MODE_")}</white> \n" +
                "<gray>Extra files mode<dark_gray>:</dark_gray> <white>${job.extraFilesMode.name.removePrefix("TEMPLATE_JOB_EXTRA_FILES_MODE_")}</white> \n" +
                "<gray>Files<dark_gray>:</dark_gray> <white>${job.filesProcessed}/${job.totalFiles}</white> " +
                "<gray>| Copied:</gray> <white>${job.filesCopied}</white> " +
                "<gray>| Skipped:</gray> <white>${job.filesSkipped}</white> " +
                "<gray>| Overwritten:</gray> <white>${job.filesOverwritten}</white> " +
                "<gray>| Purged:</gray> <white>${job.filesPurged}</white> \n" +
                "<gray>Bytes<dark_gray>:</dark_gray> <white>${formatBytes(job.bytesProcessed)}/${formatBytes(job.totalBytes)}</white>" +
                failureLine
        )
    }

    private fun jobStatusColor(status: TemplateJobStatus): String =
        when (status) {
            TemplateJobStatus.TEMPLATE_JOB_STATUS_COMPLETED -> "<green>"
            TemplateJobStatus.TEMPLATE_JOB_STATUS_FAILED -> "<red>"
            TemplateJobStatus.TEMPLATE_JOB_STATUS_CANCELLED -> "<red>"
            TemplateJobStatus.TEMPLATE_JOB_STATUS_IN_PROGRESS -> "<yellow>"
            else -> "<gray>"
        }

    private fun jobStatusColorName(status: TemplateJobStatus): String =
        when (status) {
            TemplateJobStatus.TEMPLATE_JOB_STATUS_COMPLETED -> "green"
            TemplateJobStatus.TEMPLATE_JOB_STATUS_FAILED -> "red"
            TemplateJobStatus.TEMPLATE_JOB_STATUS_CANCELLED -> "red"
            TemplateJobStatus.TEMPLATE_JOB_STATUS_IN_PROGRESS -> "yellow"
            else -> "gray"
        }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
            else -> "$bytes B"
        }
    }
}

object TemplateJobCache {
    private val cache =
        Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build<String, List<TemplateJobProgress>>()

    fun getActiveJobs(): List<TemplateJobProgress> {
        return cache.get("active") {
            runBlocking {
                Node.instance.localGrpcClient.templateAPI
                    .listActiveTemplateJobs(com.google.protobuf.Empty.getDefaultInstance())
                    .jobsList
            }
        }
    }
}
