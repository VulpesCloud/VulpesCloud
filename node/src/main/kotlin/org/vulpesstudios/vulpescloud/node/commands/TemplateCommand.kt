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
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.command.CommandSource
import org.vulpesstudios.vulpescloud.node.templates.TemplateRegistry
import org.vulpesstudios.vulpescloud.node.utils.TableUtils
import org.vulpesstudios.vulpescloud.api.templates.TemplateLocation as ApiTemplateLocation
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

class TemplateCommand {

    private val defaultTemplateColumns = listOf("id", "name", "storage", "node", "weight", "enabled")
    private val allTemplateColumns = defaultTemplateColumns + "version"
    private val storageColumns = listOf("name", "type", "node")
    private val defaultJobColumns = listOf("id", "type", "status", "source", "destination", "files")
    private val allJobColumns = defaultJobColumns + listOf("bytes", "started", "completed", "failure")

    private data class TemplateSelector(val id: String, val storage: String? = null, val node: String? = null)
    private data class StorageSelector(val name: String, val node: String? = null)

    @Suggestions("templateNames")
    fun templateNameSuggestions(): Stream<String> =
        runBlocking { TemplateRegistry.getAll().map { templateSelector(it.id, it.location.storageName, it.location.nodeName) }.stream() }

    @Suggestions("template-columns")
    fun templateColumnSuggestions(ctx: CommandContext<CommandSource>, input: CommandInput): List<String> {
        val current = input.lastRemainingToken()
        val prefix = current.substring(0, current.lastIndexOf(',') + 1)
        val selected = prefix.split(',').filter { it.isNotEmpty() }.map { it.lowercase() }.toSet()
        if ("all" in selected || "default" in selected) return emptyList()
        val candidates = if (selected.isEmpty()) allTemplateColumns + listOf("all", "default") else allTemplateColumns
        return candidates.filter { it.lowercase() !in selected }.map { prefix + it }
    }

    @Suggestions("template-storage-columns")
    fun templateStorageColumnSuggestions(ctx: CommandContext<CommandSource>, input: CommandInput): List<String> {
        val current = input.lastRemainingToken()
        val prefix = current.substring(0, current.lastIndexOf(',') + 1)
        val selected = prefix.split(',').filter { it.isNotEmpty() }.map { it.lowercase() }.toSet()
        if ("all" in selected || "default" in selected) return emptyList()
        val candidates = if (selected.isEmpty()) storageColumns + listOf("all", "default") else storageColumns
        return candidates.filter { it.lowercase() !in selected }.map { prefix + it }
    }

    @Suggestions("template-destination-storages")
    fun templateDestinationStorageSuggestions(): Stream<String> = runBlocking {
        Node.instance.templateServiceImpl.listRegisteredStorages(listRegisteredStoragesRequest {})
            .storageList
            .map { storage -> if (storage.nodeName.isBlank()) storage.name else "${storage.name}@${storage.nodeName}" }
            .distinct()
            .sorted()
            .stream()
    }

    @Suggestions("template-job-columns")
    fun templateJobColumnSuggestions(ctx: CommandContext<CommandSource>, input: CommandInput): List<String> {
        val current = input.lastRemainingToken()
        val prefix = current.substring(0, current.lastIndexOf(',') + 1)
        val selected = prefix.split(',').filter { it.isNotEmpty() }.map { it.lowercase() }.toSet()
        if ("all" in selected || "default" in selected) return emptyList()
        val candidates = if (selected.isEmpty()) allJobColumns + listOf("all", "default") else allJobColumns
        return candidates.filter { it.lowercase() !in selected }.map { prefix + it }
    }

    @Suggestions("activeTemplateJobIds")
    fun activeTemplateJobIdSuggestions(): Stream<String> =
        TemplateJobCache.getActiveJobs().map { it.jobId }.stream()

    @Suggestions("templateJobTargets")
    fun templateJobTargetSuggestions(): Stream<String> {
        val templateIds = runBlocking { TemplateRegistry.getAll().map { it.id } }
        val jobIds = TemplateJobCache.getActiveJobs().map { it.jobId }
        return (templateIds + jobIds).stream()
    }

    @Suggestions("templateConflictModes")
    fun conflictModeSuggestions(): Stream<String> =
        Stream.of("deny", "skip", "overwrite", "overwrite_newer", "overwrite_changed")

    @Suggestions("templateExtraFilesModes")
    fun extraFilesModeSuggestions(): Stream<String> =
        Stream.of("keep", "purge")

    @Permission("templates.list")
    @Command("template list [columns]")
    fun listTemplates(
        source: CommandSource,
        @Argument("columns", suggestions = "template-columns") @org.incendo.cloud.annotations.Default("default") columns: String,
        @Flag("all") all: Boolean,
    ) {
        val selected = when {
            all || columns.equals("all", ignoreCase = true) -> allTemplateColumns
            columns.equals("default", ignoreCase = true) -> defaultTemplateColumns
            else -> {
                val requested = columns.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val invalid = requested.filter { column -> allTemplateColumns.none { it.equals(column, true) } }
                if (invalid.isNotEmpty()) {
                    source.sendMessage("Unknown column(s): ${invalid.joinToString()}")
                    source.sendMessage("Available: ${allTemplateColumns.joinToString()}")
                    return
                }
                requested.map { column -> allTemplateColumns.first { it.equals(column, true) } }.distinct()
            }
        }
        runBlocking {
            val templates = Node.instance.localGrpcClient.templateAPI
                .listTemplates(listTemplatesRequest {})
                .templatesList
            val table = TableUtils.newTable().defaultStyle()
            table.header(mapOf(
                "id" to "ID", "name" to "Name", "storage" to "Storage", "node" to "Node",
                "weight" to "Weight", "enabled" to "Enabled", "version" to "Version",
            ))
            templates.forEach { template ->
                table.addContent(mapOf(
                    "id" to template.id,
                    "name" to template.name,
                    "storage" to template.location.storageName,
                    "node" to template.location.nodeName,
                    "weight" to template.weight.toString(),
                    "enabled" to template.enabled.toString(),
                    "version" to template.version,
                ))
            }
            source.sendMessage("The following ${templates.size} template(s) are registered:")
            table.lines(selected).forEach(source::sendMessage)
        }
    }

    @Permission("templates.listRegisteredStorages")
    @Command("template storages [columns]")
    fun listStorages(
        source: CommandSource,
        @Argument("columns", suggestions = "template-storage-columns") @org.incendo.cloud.annotations.Default("default") columns: String,
        @Flag("all") all: Boolean,
    ) {
        val selected = when {
            all || columns.equals("all", ignoreCase = true) || columns.equals("default", ignoreCase = true) -> storageColumns
            else -> {
                val requested = columns.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val invalid = requested.filter { column -> storageColumns.none { it.equals(column, true) } }
                if (invalid.isNotEmpty()) {
                    source.sendMessage("Unknown column(s): ${invalid.joinToString()}")
                    source.sendMessage("Available: ${storageColumns.joinToString()}")
                    return
                }
                requested.map { column -> storageColumns.first { it.equals(column, true) } }.distinct()
            }
        }
        runBlocking {
            val storages = Node.instance.localGrpcClient.templateAPI.listRegisteredStorages(
                listRegisteredStoragesRequest {}
            ).storageList
            val table = TableUtils.newTable().defaultStyle()
            table.header(mapOf("name" to "Name", "type" to "Type", "node" to "Node"))
            storages.forEach { storage ->
                table.addContent(mapOf(
                    "name" to storage.name,
                    "type" to storage.type.name,
                    "node" to storage.nodeName,
                ))
            }
            source.sendMessage("The following ${storages.size} storage(s) are registered:")
            table.lines(selected).forEach(source::sendMessage)
        }
    }

    @Permission("templates.copy")
    @Command("template copy <source> <destination> <destinationStorage>")
    fun copyTemplate(
        source: CommandSource,
        @Argument(value = "source", suggestions = "templateNames") sourceName: String,
        @Argument("destination") destinationName: String,
        @Argument(value = "destinationStorage", suggestions = "template-destination-storages") destinationStorage: String,
        @Flag(value = "conflict", suggestions = "templateConflictModes") conflictMode: String?,
        @Flag(value = "extra-files", suggestions = "templateExtraFilesModes") extraFilesMode: String?,
    ) {
        val conflict = conflictMode?.let { parseConflictMode(it, source) } ?: TemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_SKIP
        val extra = extraFilesMode?.let { parseExtraFilesMode(it, source) } ?: TemplateJobExtraFilesMode.TEMPLATE_JOB_EXTRA_FILES_MODE_KEEP

        runBlocking {
            val sourceSelector = parseTemplateSelector(sourceName)
                ?: run { source.sendMessage("<red>Use template selectors in the form id@storage[@node]</red>"); return@runBlocking }
            val sourceTemplate = TemplateRegistry.getById(sourceSelector.id)
                ?: run { source.sendMessage("<red>Template id not found: ${sourceSelector.id}</red>"); return@runBlocking }
            if (!sourceSelector.matches(sourceTemplate.id, sourceTemplate.location.storageName, sourceTemplate.location.nodeName)) {
                source.sendMessage("<red>Template '$sourceName' does not exist in that storage/node</red>")
                return@runBlocking
            }
            val destinationLocation = resolveDestinationStorage(destinationStorage, source)
                ?: return@runBlocking

            val request = copyTemplateRequest {
                this.source = templateReference {
                    templateId = sourceTemplate.id
                    name = sourceTemplate.name
                    location = sourceTemplate.location.toDefinition()
                }
                this.destination = templateReference {
                    name = destinationName
                    location = destinationLocation.toDefinition()
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
    @Command("template move <source> <destination> <destinationStorage>")
    fun moveTemplate(
        source: CommandSource,
        @Argument(value = "source", suggestions = "templateNames") sourceName: String,
        @Argument("destination") destinationName: String,
        @Argument(value = "destinationStorage", suggestions = "template-destination-storages") destinationStorage: String,
        @Flag(value = "conflict", suggestions = "templateConflictModes") conflictMode: String?,
        @Flag(value = "extra-files", suggestions = "templateExtraFilesModes") extraFilesMode: String?,
    ) {
        val conflict = conflictMode?.let { parseConflictMode(it, source) } ?: TemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_SKIP
        val extra = extraFilesMode?.let { parseExtraFilesMode(it, source) } ?: TemplateJobExtraFilesMode.TEMPLATE_JOB_EXTRA_FILES_MODE_KEEP

        runBlocking {
            val sourceSelector = parseTemplateSelector(sourceName)
                ?: run { source.sendMessage("<red>Use template selectors in the form id@storage[@node]</red>"); return@runBlocking }
            val sourceTemplate = TemplateRegistry.getById(sourceSelector.id)
                ?: run { source.sendMessage("<red>Template id not found: ${sourceSelector.id}</red>"); return@runBlocking }
            if (!sourceSelector.matches(sourceTemplate.id, sourceTemplate.location.storageName, sourceTemplate.location.nodeName)) {
                source.sendMessage("<red>Template '$sourceName' does not exist in that storage/node</red>")
                return@runBlocking
            }
            val destinationLocation = resolveDestinationStorage(destinationStorage, source)
                ?: return@runBlocking

            val request = moveTemplateRequest {
                this.source = templateReference {
                    templateId = sourceTemplate.id
                    name = sourceTemplate.name
                    location = sourceTemplate.location.toDefinition()
                }
                this.destination = templateReference {
                    name = destinationName
                    location = destinationLocation.toDefinition()
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
    @Command("template job list [columns]")
    fun listTemplateJobs(
        source: CommandSource,
        @Argument("columns", suggestions = "template-job-columns") @org.incendo.cloud.annotations.Default("default") columns: String,
        @Flag("all") all: Boolean,
    ) {
        val selected = when {
            all || columns.equals("all", ignoreCase = true) -> allJobColumns
            columns.equals("default", ignoreCase = true) -> defaultJobColumns
            else -> {
                val requested = columns.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val invalid = requested.filter { column -> allJobColumns.none { it.equals(column, true) } }
                if (invalid.isNotEmpty()) {
                    source.sendMessage("Unknown column(s): ${invalid.joinToString()}")
                    source.sendMessage("Available: ${allJobColumns.joinToString()}")
                    return
                }
                requested.map { column -> allJobColumns.first { it.equals(column, true) } }.distinct()
            }
        }
        runBlocking {
            val jobs = Node.instance.templateServiceImpl.listAllTemplateJobs()

            if (jobs.isEmpty()) {
                source.sendMessage("<gray>There are no template jobs.</gray>")
                return@runBlocking
            }

            val table = TableUtils.newTable().defaultStyle()
            table.header(mapOf(
                "id" to "Job ID", "type" to "Type", "status" to "Status", "source" to "Source",
                "destination" to "Destination", "files" to "Files", "bytes" to "Bytes",
                "started" to "Started", "completed" to "Completed", "failure" to "Failure",
            ))
            jobs.sortedByDescending { it.startedAt }.forEach { job ->
                table.addContent(mapOf(
                    "id" to job.jobId,
                    "type" to job.type.name.removePrefix("TEMPLATE_JOB_TYPE_"),
                    "status" to job.status.name.removePrefix("TEMPLATE_JOB_STATUS_"),
                    "source" to formatJobTemplate(job.source),
                    "destination" to formatJobTemplate(job.destination),
                    "files" to "${job.filesProcessed}/${job.totalFiles} (copied ${job.filesCopied}, skipped ${job.filesSkipped})",
                    "bytes" to "${formatBytes(job.bytesProcessed)}/${formatBytes(job.totalBytes)}",
                    "started" to job.startedAt.toString(),
                    "completed" to job.completedAt.takeIf { it > 0 }?.toString().orEmpty(),
                    "failure" to job.failureReason,
                ))
            }
            source.sendMessage("The following ${jobs.size} template job(s) are registered:")
            table.lines(selected).forEach(source::sendMessage)
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

        // Otherwise treat it as a template id and search active jobs.
        val jobs = Node.instance.localGrpcClient.templateAPI
            .listActiveTemplateJobs(com.google.protobuf.Empty.getDefaultInstance())
            .jobsList

        // Otherwise treat as template id — find a job involving this template.
        jobs.find { it.source.templateId == target || it.destination.templateId == target }?.let { return it }

        source.sendMessage("<red>No active job found for</red> <white>$target</white><red>.</red>")
        return null
    }

    private fun parseTemplateSelector(value: String): TemplateSelector? {
        val parts = value.split('@')
        if (parts.isEmpty() || parts.size > 3 || parts[0].isBlank()) return null
        if (parts.size > 1 && parts[1].isBlank()) return null
        if (parts.size > 2 && parts[2].isBlank()) return null
        return TemplateSelector(
            id = parts[0],
            storage = parts.getOrNull(1),
            node = parts.getOrNull(2),
        )
    }

    private suspend fun resolveDestinationStorage(value: String, source: CommandSource): ApiTemplateLocation? {
        val parts = value.split('@')
        if (parts.isEmpty() || parts.size > 2 || parts[0].isBlank() || (parts.size == 2 && parts[1].isBlank())) {
            source.sendMessage("<red>Use a destination storage in the form storage[@node]</red>")
            return null
        }
        val selector = StorageSelector(parts[0], parts.getOrNull(1))
        val candidates = Node.instance.templateServiceImpl
            .listRegisteredStorages(listRegisteredStoragesRequest {})
            .storageList
            .filter { it.name.equals(selector.name, ignoreCase = true) }
            .filter { selector.node == null || it.nodeName == selector.node }
        if (candidates.isEmpty()) {
            source.sendMessage("<red>Template storage '$value' is not registered</red>")
            return null
        }
        val selected = if (selector.node != null) candidates.singleOrNull() else {
            candidates.singleOrNull { it.nodeName.isBlank() } ?: candidates.singleOrNull()
        }
        if (selected == null) {
            source.sendMessage("<red>Storage '${selector.name}' is registered on multiple nodes; select one of the suggested storage@node values</red>")
            return null
        }
        return ApiTemplateLocation(selected.name, selected.nodeName.takeIf { it.isNotBlank() })
    }

    private fun templateSelector(id: String, storage: String, node: String?): String =
        buildString {
            append(id)
            if (storage.isNotBlank()) {
                append('@').append(storage)
                if (!node.isNullOrBlank()) append('@').append(node)
            }
        }

    private fun formatJobTemplate(reference: TemplateReference): String {
        val location = buildString {
            append(reference.location.storageName)
            if (reference.location.nodeName.isNotBlank()) append('@').append(reference.location.nodeName)
        }
        return "${reference.name} (${reference.templateId})@$location"
    }

    private fun TemplateSelector.matches(id: String, storage: String, node: String?): Boolean =
        this.id == id && (this.storage == null || this.storage.equals(storage, ignoreCase = true)) &&
            (this.node == null || this.node == node)

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
                "<gray>Source<dark_gray>:</dark_gray> <white>${job.source.templateId}</white> <gray>(${job.source.name})</gray> " +
                "<gray>@</gray> <white>${job.source.location.storageName}</white> \n" +
                "<gray>Destination<dark_gray>:</dark_gray> <white>${job.destination.templateId}</white> <gray>(${job.destination.name})</gray> " +
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
