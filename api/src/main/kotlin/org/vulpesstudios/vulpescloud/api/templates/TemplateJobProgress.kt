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

package org.vulpesstudios.vulpescloud.api.templates

import kotlinx.serialization.Serializable
import build.buf.gen.vulpescloud.templates.v1.TemplateJobProgress as ProtoTemplateJobProgress
import build.buf.gen.vulpescloud.templates.v1.TemplateReference as ProtoTemplateReference

@Serializable
data class TemplateJobProgress(
    val jobId: String,
    val type: TemplateJobType = TemplateJobType.COPY,
    val status: TemplateJobStatus = TemplateJobStatus.PENDING,
    val source: TemplateReferenceData = TemplateReferenceData("", "", TemplateLocation("", null)),
    val destination: TemplateReferenceData = TemplateReferenceData("", "", TemplateLocation("", null)),
    val conflictMode: TemplateJobConflictMode = TemplateJobConflictMode.SKIP,
    val extraFilesMode: TemplateJobExtraFilesMode = TemplateJobExtraFilesMode.KEEP,
    val totalFiles: Int = 0,
    val filesProcessed: Int = 0,
    val bytesProcessed: Long = 0,
    val totalBytes: Long = 0,
    val currentFile: String = "",
    val filesCopied: Int = 0,
    val filesSkipped: Int = 0,
    val filesOverwritten: Int = 0,
    val filesPurged: Int = 0,
    val startedAt: Long = 0,
    val completedAt: Long = 0,
    val failureReason: String? = null,
) {
    fun toDefinition(): ProtoTemplateJobProgress {
        val builder =
            ProtoTemplateJobProgress.newBuilder()
                .setJobId(jobId)
                .setType(type.toDefinition())
                .setStatus(status.toDefinition())
                .setSource(source.toReference())
                .setDestination(destination.toReference())
                .setConflictMode(conflictMode.toDefinition())
                .setExtraFilesMode(extraFilesMode.toDefinition())
                .setTotalFiles(totalFiles)
                .setFilesProcessed(filesProcessed)
                .setBytesProcessed(bytesProcessed)
                .setTotalBytes(totalBytes)
                .setCurrentFile(currentFile)
                .setFilesCopied(filesCopied)
                .setFilesSkipped(filesSkipped)
                .setFilesOverwritten(filesOverwritten)
                .setFilesPurged(filesPurged)
                .setStartedAt(startedAt)
                .setCompletedAt(completedAt)

        failureReason?.let { builder.setFailureReason(it) }

        return builder.build()
    }

    companion object {
        fun fromDefinition(definition: ProtoTemplateJobProgress): TemplateJobProgress =
            TemplateJobProgress(
                jobId = definition.jobId,
                type = TemplateJobType.fromDefinition(definition.type),
                status = TemplateJobStatus.fromDefinition(definition.status),
                source = TemplateReferenceData.fromReference(definition.source),
                destination = TemplateReferenceData.fromReference(definition.destination),
                conflictMode = TemplateJobConflictMode.fromDefinition(definition.conflictMode),
                extraFilesMode = TemplateJobExtraFilesMode.fromDefinition(definition.extraFilesMode),
                totalFiles = definition.totalFiles,
                filesProcessed = definition.filesProcessed,
                bytesProcessed = definition.bytesProcessed,
                totalBytes = definition.totalBytes,
                currentFile = definition.currentFile,
                filesCopied = definition.filesCopied,
                filesSkipped = definition.filesSkipped,
                filesOverwritten = definition.filesOverwritten,
                filesPurged = definition.filesPurged,
                startedAt = definition.startedAt,
                completedAt = definition.completedAt,
                failureReason = if (definition.failureReason.isNotBlank()) definition.failureReason else null,
            )
    }
}

@Serializable
data class TemplateReferenceData(
    val templateId: String,
    val name: String,
    val location: TemplateLocation,
) {
    fun toReference(): ProtoTemplateReference =
        ProtoTemplateReference.newBuilder()
            .setTemplateId(templateId)
            .setName(name)
            .setLocation(location.toDefinition())
            .build()

    companion object {
        fun fromReference(ref: ProtoTemplateReference): TemplateReferenceData =
            TemplateReferenceData(
                templateId = ref.templateId,
                name = ref.name,
                location = TemplateLocation.fromDefinition(ref.location),
            )
    }
}
