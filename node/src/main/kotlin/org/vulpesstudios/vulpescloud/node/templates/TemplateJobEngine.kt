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

package org.vulpesstudios.vulpescloud.node.templates

import kotlinx.coroutines.*
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.templates.*
import org.vulpesstudios.vulpescloud.node.NodeCoroutineScope
import java.util.*
import build.buf.gen.vulpescloud.templates.v1.TemplateJobConflictMode as ProtoTemplateJobConflictMode
import build.buf.gen.vulpescloud.templates.v1.TemplateJobExtraFilesMode as ProtoTemplateJobExtraFilesMode
import build.buf.gen.vulpescloud.templates.v1.TemplateJobProgress as ProtoTemplateJobProgress
import build.buf.gen.vulpescloud.templates.v1.TemplateJobType as ProtoTemplateJobType
import build.buf.gen.vulpescloud.templates.v1.TemplateReference
import build.buf.gen.vulpescloud.templates.v1.CreateTemplateRequest
import build.buf.gen.vulpescloud.templates.v1.CreateDirectoryRequest
import build.buf.gen.vulpescloud.templates.v1.DeleteFileRequest
import build.buf.gen.vulpescloud.templates.v1.DeleteTemplateRequest
import build.buf.gen.vulpescloud.templates.v1.ListDirectoryRequest
import build.buf.gen.vulpescloud.templates.v1.ReadFileRequest
import build.buf.gen.vulpescloud.templates.v1.TemplateDirectoryEntry
import build.buf.gen.vulpescloud.templates.v1.UpdateFileRequest

class TemplateJobEngine(
    private val storage: TemplateJobStorage = TemplateJobStorage(),
    private val lockStorage: TemplateJobLockStorage = TemplateJobLockStorage(),
) {
    private companion object {
        const val WORKER_LEASE_MILLIS = 15_000L
        const val WORKER_RENEW_INTERVAL_MILLIS = 5_000L
        const val ORPHAN_LOCK_GRACE_MILLIS = 30_000L
    }

    // Lazy reference to avoid circular dependency
    private var templateServiceRef: (() -> TemplateServiceImpl)? = null

    fun setTemplateService(provider: () -> TemplateServiceImpl) {
        templateServiceRef = provider
    }

    private val templateService: TemplateServiceImpl
        get() = templateServiceRef?.invoke()
            ?: throw IllegalStateException("TemplateService not set on TemplateJobEngine")
    private val logger = LoggerFactory.getLogger(TemplateJobEngine::class.java)
    private val jobHandles: MutableMap<String, Job> = java.util.concurrent.ConcurrentHashMap()

    /**
     * Validates a job request and returns an accept response.
     * For DENY conflict mode, performs a pre-scan to check for conflicts.
     */
    suspend fun validateAndAccept(
        jobType: TemplateJobType,
        sourceRef: TemplateReference,
        destinationRef: TemplateReference,
        conflictMode: TemplateJobConflictMode,
        extraFilesMode: TemplateJobExtraFilesMode,
    ): TemplateJobAcceptResult {
        // Check source exists
        val sourceTemplate = TemplateRegistry.findByReference(sourceRef)
            ?: return TemplateJobAcceptResult(
                TemplateJobAcceptStatus.REJECTED_SOURCE_NOT_FOUND,
                "Source template not found"
            )

        val destinationById = destinationRef.templateId.takeIf { it.isNotBlank() }
            ?.let { TemplateRegistry.getById(it) }
        if (destinationById != null && !TemplateRegistry.matchesLocation(
                destinationById.location.toDefinition(), destinationRef.location
            )
        ) {
            return TemplateJobAcceptResult(
                TemplateJobAcceptStatus.REJECTED_INVALID_CONFIGURATION,
                "Destination template id '${destinationRef.templateId}' exists in a different storage/node"
            )
        }
        val destinationCandidates = if (destinationRef.name.isBlank()) emptyList() else
            TemplateRegistry.getAll().filter {
                it.name == destinationRef.name &&
                    TemplateRegistry.matchesLocation(it.location.toDefinition(), destinationRef.location)
            }
        if (destinationCandidates.size > 1) {
            return TemplateJobAcceptResult(
                TemplateJobAcceptStatus.REJECTED_INVALID_CONFIGURATION,
                "Destination name '${destinationRef.name}' matches multiple templates in the selected storage/node"
            )
        }
        val existingDestination = TemplateRegistry.findByReference(destinationRef)

        if (destinationRef.templateId.isBlank() && destinationRef.name.isNotBlank() &&
            destinationRef.location.storageName.isBlank() && destinationRef.location.nodeName.isBlank() &&
            existingDestination == null && destinationCandidates.isEmpty() &&
            TemplateRegistry.getAll().count { it.name == destinationRef.name } > 1
        ) {
            return TemplateJobAcceptResult(
                TemplateJobAcceptStatus.REJECTED_INVALID_CONFIGURATION,
                "Destination name is ambiguous; specify its storage/node or use a unique name"
            )
        }

        if (destinationById?.id == sourceTemplate.id || existingDestination?.id == sourceTemplate.id) {
            return TemplateJobAcceptResult(
                TemplateJobAcceptStatus.REJECTED_INVALID_CONFIGURATION,
                "Source and destination resolve to the same template"
            )
        }

        // For DENY mode, pre-scan for conflicts
        if (conflictMode == TemplateJobConflictMode.DENY) {
            val destToScan = existingDestination
            if (destToScan != null) {
                try {
                    val conflicts = hasFileConflicts(sourceTemplate, destToScan)
                    if (conflicts) {
                        return TemplateJobAcceptResult(
                            TemplateJobAcceptStatus.REJECTED_FILES_CONFLICT,
                            "Files in destination conflict with source and conflict mode is DENY"
                        )
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to pre-scan destination for conflicts", e)
                    // Fall through to async execution if scan fails
                }
            }
        }

        // Create the job
        val jobId = UUID.randomUUID().toString()
        val requestedDestinationLocation = when {
            destinationRef.location.storageName.isNotBlank() ->
                TemplateLocation(destinationRef.location.storageName, destinationRef.location.nodeName)
            existingDestination != null -> existingDestination.location
            else -> sourceTemplate.location
        }
        val destinationLocation = templateService.resolveLocation(
            requestedDestinationLocation.storageName,
            requestedDestinationLocation.nodeName,
        )
        val sourceData = TemplateReferenceData(
            sourceTemplate.id,
            sourceTemplate.name,
            sourceTemplate.location,
        )
        val destinationName = destinationRef.name.ifBlank { sourceTemplate.name }
        val proposedDestinationId = destinationRef.templateId.ifBlank { Template.normalizedId(destinationName) }
        if (existingDestination == null && destinationRef.templateId.isBlank() && proposedDestinationId.isBlank()) {
            return TemplateJobAcceptResult(
                TemplateJobAcceptStatus.REJECTED_INVALID_CONFIGURATION,
                "Destination name must contain at least one letter or number to generate an id"
            )
        }
        if (existingDestination == null && destinationRef.templateId.isBlank() &&
            TemplateRegistry.getById(proposedDestinationId) != null
        ) {
            return TemplateJobAcceptResult(
                TemplateJobAcceptStatus.REJECTED_INVALID_CONFIGURATION,
                "Generated destination id '$proposedDestinationId' is already in use; choose a different name or specify an id"
            )
        }
        val destData = existingDestination?.let {
            TemplateReferenceData(it.id, it.name, it.location)
        } ?: TemplateReferenceData(
            proposedDestinationId,
            destinationName,
            destinationLocation,
        )
        val now = System.currentTimeMillis()

        val progress = TemplateJobProgress(
            jobId = jobId,
            type = jobType,
            status = TemplateJobStatus.PENDING,
            source = sourceData,
            destination = destData,
            conflictMode = conflictMode,
            extraFilesMode = extraFilesMode,
            startedAt = now,
        )

        val destinationLockKeys = if (existingDestination == null) {
            setOf(TemplateLockKey.template(destData.templateId))
        } else {
            setOf(TemplateLockKey.template(existingDestination.id))
        }
        val lockKeys = destinationLockKeys + TemplateLockKey.template(sourceTemplate.id)
        val record = TemplateJobRecord(
            progress = progress,
            destinationExisted = existingDestination != null,
            lockKeys = lockKeys,
        )
        if (!lockStorage.acquire(lockKeys, jobId, "${jobType.name.lowercase()} ${sourceTemplate.name} → ${destData.name}")) {
            return TemplateJobAcceptResult(
                TemplateJobAcceptStatus.REJECTED_INVALID_CONFIGURATION,
                "Source or destination template is locked by another job"
            )
        }
        try {
            storage.create(record)
        } catch (e: Exception) {
            lockStorage.release(lockKeys, jobId)
            throw e
        }
        return TemplateJobAcceptResult(
            TemplateJobAcceptStatus.ACCEPTED,
            "",
            progress
        )
    }

    /**
     * Starts executing a job asynchronously.
     */
    suspend fun execute(progress: TemplateJobProgress) {
        val nodeName = org.vulpesstudios.vulpescloud.node.Node.instance.configProvider.config.nodeName
        val now = System.currentTimeMillis()
        val claimed = storage.claim(progress.jobId, nodeName, now, WORKER_LEASE_MILLIS) ?: return
        val current = claimed.progress
        storage.save(current.copy(status = TemplateJobStatus.IN_PROGRESS, startedAt = now))

        // Publish started event
        if (current.status == TemplateJobStatus.PENDING) {
            publishEvent("started", progress.jobId) {
                TemplateJobEvents.started(current.copy(status = TemplateJobStatus.IN_PROGRESS, startedAt = now))
            }
        }

        val job = NodeCoroutineScope.launch(
            TemplateJobCallContext.CoroutineElement(TemplateJobIdentity(progress.jobId, claimed.workerToken))
        ) {
            val leaseRenewal = launch {
                while (isActive) {
                    delay(WORKER_RENEW_INTERVAL_MILLIS)
                    storage.updateRecord(progress.jobId) { record ->
                        if (record == null || record.workerToken != claimed.workerToken ||
                            record.workerNodeName != nodeName || !record.progress.status.isActive
                        ) {
                            record ?: claimed
                        } else {
                            record.copy(workerLeaseUntil = System.currentTimeMillis() + WORKER_LEASE_MILLIS)
                        }
                    }
                }
            }
            try {
                runJob(claimed.progress)
            } catch (e: CancellationException) {
                val currentRecord = withContext(NonCancellable) {
                    storage.getRecord(progress.jobId)
                } ?: return@launch
                if (currentRecord.workerToken != claimed.workerToken) return@launch
                val current = currentRecord.progress
                if (!current.status.isActive) return@launch
                if (currentRecord.cancelRequested) {
                    logger.info("Job ${progress.jobId} was cancelled by request")
                    val cancelled = current.copy(
                        status = TemplateJobStatus.CANCELLED,
                        completedAt = System.currentTimeMillis()
                    )
                    withContext(NonCancellable) { storage.save(cancelled) }
                    publishEvent("cancelled", progress.jobId) { TemplateJobEvents.cancelled(cancelled) }
                } else {
                    logger.info("Job ${progress.jobId} stopped with its node and can be recovered")
                    withContext(NonCancellable) {
                        storage.updateRecord(progress.jobId) { record ->
                            val currentRecordState = record ?: currentRecord
                            if (currentRecordState.workerToken != claimed.workerToken) currentRecordState
                            else currentRecordState.copy(workerNodeName = null, workerLeaseUntil = 0)
                        }
                    }
                }
            } catch (e: Exception) {
                if (isTemporaryAvailabilityFailure(e)) {
                    logger.warn("Template job {} is waiting for a template host/storage to become available: {}", progress.jobId, e.message)
                    storage.updateRecord(progress.jobId) { record ->
                        val current = record ?: claimed
                        current.copy(
                            progress = current.progress.copy(status = TemplateJobStatus.IN_PROGRESS, completedAt = 0),
                            workerNodeName = null,
                            workerLeaseUntil = 0,
                        )
                    }
                } else {
                    logger.error("Job ${progress.jobId} failed", e)
                    val currentRecord = storage.getRecord(progress.jobId) ?: return@launch
                    if (currentRecord.workerToken != claimed.workerToken) return@launch
                    val current = currentRecord.progress
                    if (!current.status.isActive) return@launch
                    val failed = current.copy(
                        status = TemplateJobStatus.FAILED,
                        failureReason = e.message ?: "Unknown error",
                        completedAt = System.currentTimeMillis()
                    )
                    storage.save(failed)
                    publishEvent("failed", progress.jobId) { TemplateJobEvents.failed(failed) }
                }
            } finally {
                leaseRenewal.cancel()
                jobHandles.remove(progress.jobId, coroutineContext[Job])
            }
        }
        jobHandles[progress.jobId] = job
        // A very small job can finish before launch() returns and before it is put in the map.
        // In that case the coroutine's finally block ran too early to remove this handle.
        if (!job.isActive) jobHandles.remove(progress.jobId, job)
    }

    /**
     * Cancels a running job.
     */
    suspend fun cancel(jobId: String): Boolean {
        val job = jobHandles[jobId]
        if (job?.isActive == true && storage.get(jobId)?.status?.isActive == true) {
            val requested = storage.updateRecord(jobId) { record ->
                val current = record ?: throw IllegalStateException("Template job $jobId disappeared")
                if (!current.progress.status.isActive) current else current.copy(cancelRequested = true)
            }
            if (requested?.cancelRequested != true) return false
            job.cancel()
            return true
        }
        return false
    }

    private inline fun publishEvent(event: String, jobId: String, publish: () -> Unit) {
        try {
            publish()
        } catch (e: Exception) {
            // Event delivery is ancillary; it must not prevent a job from starting or change a
            // completed operation into a failed job.
            logger.warn("Failed to publish template job {} event for {}", event, jobId, e)
        }
    }

    /** Called periodically on every node; the database lease elects a single recovery worker. */
    suspend fun reconcile() {
        val localNode = org.vulpesstudios.vulpescloud.node.Node.instance.configProvider.config.nodeName
        val now = System.currentTimeMillis()
        // Lock first, then job record is the safe acceptance ordering. A crash between those
        // writes can leave an orphan lock; release it only after a grace period and only when
        // its owner record is still absent.
        for ((key, lock) in lockStorage.getAll()) {
            if (!lock.description.startsWith("copy ") && !lock.description.startsWith("move ")) continue
            if (now - lock.lockedAt < ORPHAN_LOCK_GRACE_MILLIS) continue
            if (storage.getRecord(lock.jobId) == null) {
                lockStorage.release(setOf(key), lock.jobId)
            }
        }
        for (record in storage.getAllRecords()) {
            if (record.progress.status == TemplateJobStatus.COMPLETED) {
                val keys = lockKeysFor(record)
                lockStorage.release(keys, record.progress.jobId)
                if (record.lockKeys.isNotEmpty()) {
                    storage.updateRecord(record.progress.jobId) { current ->
                        (current ?: record).copy(lockKeys = emptySet(), workerLeaseUntil = 0)
                    }
                }
                continue
            }
            if (!record.progress.status.isActive || record.workerLeaseUntil > now) continue
            if (!hostsEitherLocation(record.progress, localNode)) continue
            if (!canReachBothLocations(record.progress, localNode)) continue
            val keys = lockKeysFor(record)
            if (!lockStorage.acquire(keys, record.progress.jobId, "recovered template job ${record.progress.jobId}")) {
                storage.save(record.progress.copy(
                    status = TemplateJobStatus.FAILED,
                    failureReason = "manual_recovery_required: template lock is owned by another job; locks retained",
                    completedAt = System.currentTimeMillis(),
                ))
                continue
            }
            storage.updateRecord(record.progress.jobId) { current ->
                (current ?: record).copy(lockKeys = keys)
            }
            execute(record.progress)
        }
    }

    private fun hostsEitherLocation(progress: TemplateJobProgress, localNode: String): Boolean {
        fun hosts(location: TemplateLocation): Boolean {
            if (location.nodeName == localNode) return true
            return location.nodeName.isNullOrBlank() &&
                TemplateStorageRegistry.getTemplateStorageByName(location.storageName) != null
        }
        return hosts(progress.source.location) || hosts(progress.destination.location)
    }

    private suspend fun canReachBothLocations(progress: TemplateJobProgress, localNode: String): Boolean {
        suspend fun canReach(location: TemplateLocation): Boolean {
            val node = location.nodeName
            if (node.isNullOrBlank()) {
                return TemplateStorageRegistry.getTemplateStorageByName(location.storageName) != null
            }
            if (node == localNode) return TemplateStorageRegistry.getTemplateStorageByName(location.storageName) != null
            return templateService.remoteStubForRecovery(node) != null
        }
        return canReach(progress.source.location) && canReach(progress.destination.location)
    }

    private fun isTemporaryAvailabilityFailure(error: Exception): Boolean {
        var cause: Throwable? = error
        while (cause != null) {
            val status = when (cause) {
                is StatusException -> cause.status
                is StatusRuntimeException -> cause.status
                else -> null
            }
            if (status?.code == io.grpc.Status.Code.UNAVAILABLE ||
                cause.message?.contains("not reachable", ignoreCase = true) == true
            ) return true
            cause = cause.cause
        }
        return false
    }

    suspend fun unlockForManualRecovery(jobId: String): Boolean {
        val record = storage.getRecord(jobId) ?: return false
        if (record.progress.status.isActive) return false
        val keys = lockKeysFor(record)
        val held = lockStorage.getAll().any { (key, lock) -> key in keys && lock.jobId == jobId }
        if (!held) return false
        lockStorage.release(keys, jobId)
        return storage.updateRecord(jobId) { current ->
            (current ?: record).copy(lockKeys = emptySet())
        } != null
    }

    suspend fun cancelUnclaimed(jobId: String): Boolean {
        val completedAt = System.currentTimeMillis()
        val updated = storage.updateRecord(jobId) { current ->
            val record = current ?: throw IllegalStateException("Template job $jobId disappeared")
            if (!record.progress.status.isActive || record.workerLeaseUntil > completedAt) {
                record
            } else {
                record.copy(
                    progress = record.progress.copy(
                    status = TemplateJobStatus.CANCELLED,
                    completedAt = completedAt,
                    ),
                    workerNodeName = null,
                    workerLeaseUntil = 0,
                )
            }
        } ?: return false
        if (updated.progress.status == TemplateJobStatus.CANCELLED && updated.progress.completedAt == completedAt) {
            publishEvent("cancelled", jobId) { TemplateJobEvents.cancelled(updated.progress) }
            return true
        }
        return false
    }

    private suspend fun runJob(progress: TemplateJobProgress) {
        var record = storage.getRecord(progress.jobId) ?: return
        val sourceTemplate = TemplateRegistry.findByReference(record.progress.source.toReference())
        if (sourceTemplate == null && record.phase == TemplateJobPhase.DELETING_SOURCE) {
            val destination = TemplateRegistry.findByReference(record.progress.destination.toReference())
                ?: throw unsafeRecovery("destination template metadata is missing")
            validateDestinationInventory(record, destination)
            completeJob(record)
            return
        }
        val source = sourceTemplate ?: throw unsafeRecovery("source template metadata is missing")
        val destinationReference = record.progress.destination.toReference()
        var destination = TemplateRegistry.findByReference(destinationReference)

        if (record.phase == TemplateJobPhase.ACCEPTED) {
            val sourceInventory = snapshotFiles(source)
            val initialDestination = destination?.let { snapshotFiles(it) } ?: emptyList()
            record = updateRecord(record.progress.jobId) {
                it.copy(
                    phase = TemplateJobPhase.COPYING,
                    sourceInventory = sourceInventory,
                    initialDestinationInventory = initialDestination,
                    expectedDestinationInventory = initialDestination,
                    progress = it.progress.copy(
                        totalFiles = sourceInventory.size,
                        totalBytes = sourceInventory.sumOf { file -> file.size },
                    ),
                )
            }
        } else {
            val currentSource = snapshotFiles(source)
            if (record.phase == TemplateJobPhase.DELETING_SOURCE) {
                if (currentSource.any { it !in record.sourceInventory }) {
                    throw unsafeRecovery("source contains files outside the saved move inventory")
                }
            } else if (currentSource != record.sourceInventory) {
                throw unsafeRecovery("source path/size/modified-time inventory changed")
            }
        }

        if (destination == null) {
            if (record.destinationExisted || record.expectedDestinationInventory.isNotEmpty()) {
                throw unsafeRecovery("destination template metadata disappeared")
            }
            val draft = source.copy(
                id = record.progress.destination.templateId,
                name = record.progress.destination.name,
                location = record.progress.destination.location,
            )
            destination = Template.fromDefinition(
                templateService.createTemplate(
                    CreateTemplateRequest.newBuilder()
                        .setTemplate(draft.toDefinition())
                        .setDestination(draft.location.toDefinition())
                        .build()
                ).template
            )
        }

        record = updateRecord(progress.jobId) { current ->
            current.copy(
                progress = current.progress.copy(
                    destination = TemplateReferenceData(destination.id, destination.name, destination.location)
                )
            )
        }

        validateRecoveryCheckpoint(record, source, destination)
        record = storage.getRecord(progress.jobId) ?: return

        val sourceEntries = mutableListOf<TemplateDirectoryEntry>()
        scanDirectory(source, "", sourceEntries)
        for (entry in sourceEntries.filter { it.directory }.sortedBy { it.path.count { c -> c == '/' } }) {
            val response = templateService.createDirectory(
                CreateDirectoryRequest.newBuilder()
                    .setTemplate(templateReferenceOf(destination))
                    .setPath(entry.path)
                    .build()
            )
            check(response.result.success) {
                "Failed to create directory '${entry.path}': ${response.result.message}"
            }
        }

        val sourceFiles = sourceEntries.filterNot { it.directory }
        for (entry in sourceFiles) {
            record = storage.getRecord(progress.jobId) ?: return
            if (!record.progress.status.isActive) return
            if (entry.path in record.completedPaths) continue

            val existing = record.expectedDestinationInventory.firstOrNull { it.path == entry.path }
            val shouldCopy = when (record.progress.conflictMode) {
                TemplateJobConflictMode.DENY -> {
                    if (entry.path in record.initialDestinationInventory.map { it.path }) {
                        throw unsafeRecovery("DENY conflict appeared for '${entry.path}'")
                    }
                    true
                }
                TemplateJobConflictMode.SKIP -> existing == null
                TemplateJobConflictMode.OVERWRITE -> true
                TemplateJobConflictMode.OVERWRITE_NEWER -> existing == null || entry.modifiedAt > existing.modifiedAt
                TemplateJobConflictMode.OVERWRITE_CHANGED -> existing == null || entry.size != existing.size || entry.modifiedAt != existing.modifiedAt
            }

            if (!shouldCopy) {
                record = updateRecord(progress.jobId) { current ->
                    current.copy(
                        completedPaths = current.completedPaths + entry.path,
                        progress = current.progress.copy(
                            filesProcessed = current.progress.filesProcessed + 1,
                            filesSkipped = current.progress.filesSkipped + 1,
                            currentFile = entry.path,
                        ),
                    )
                }
                continue
            }

            record = updateRecord(progress.jobId) { it.copy(inFlightPath = entry.path) }
            try {
                val file = templateService.readFile(
                    ReadFileRequest.newBuilder()
                        .setTemplate(templateReferenceOf(source))
                        .setPath(entry.path)
                        .build()
                ).file
                check(file.size == entry.size && file.modifiedAt == entry.modifiedAt) {
                    "Source file '${entry.path}' changed after the job checkpoint"
                }
                val response = templateService.updateFile(
                    UpdateFileRequest.newBuilder()
                        .setTemplate(templateReferenceOf(destination))
                        .setPath(entry.path)
                        .setContent(file.content)
                        .build()
                )
                check(response.result.success) { "Failed to update '${entry.path}': ${response.result.message}" }

                val destinationSnapshot = snapshotFile(destination, entry.path)
                    ?: throw IllegalStateException("Destination file '${entry.path}' is missing after write")
                val wasOverwrite = record.initialDestinationInventory.any { it.path == entry.path }
                record = updateRecord(progress.jobId) { current ->
                    current.copy(
                        expectedDestinationInventory = current.expectedDestinationInventory
                            .filterNot { it.path == entry.path } + destinationSnapshot,
                        completedPaths = current.completedPaths + entry.path,
                        inFlightPath = null,
                        progress = current.progress.copy(
                            filesProcessed = current.progress.filesProcessed + 1,
                            bytesProcessed = current.progress.bytesProcessed + file.size,
                            filesCopied = current.progress.filesCopied + if (wasOverwrite) 0 else 1,
                            filesOverwritten = current.progress.filesOverwritten + if (wasOverwrite) 1 else 0,
                            currentFile = entry.path,
                        ),
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                throw IllegalStateException("Failed to copy file '${entry.path}'", e)
            }
        }

        record = storage.getRecord(progress.jobId) ?: return
        if (record.progress.type == TemplateJobType.MOVE && record.progress.filesSkipped > 0) {
            throw IllegalStateException(
                "Move stopped because files were skipped; source is retained and both templates remain locked for manual recovery"
            )
        }

        if (record.progress.extraFilesMode == TemplateJobExtraFilesMode.PURGE && record.destinationExisted) {
            record = updateRecord(progress.jobId) { it.copy(phase = TemplateJobPhase.PURGING) }
            val sourcePaths = record.sourceInventory.map { it.path }.toSet()
            val extras = record.expectedDestinationInventory.filter { it.path !in sourcePaths }
            for (extra in extras) {
                record = storage.getRecord(progress.jobId) ?: return
                if (extra.path in record.purgedPaths) continue
                record = updateRecord(progress.jobId) { it.copy(inFlightPurgePath = extra.path) }
                val response = templateService.deleteFile(
                    DeleteFileRequest.newBuilder()
                        .setTemplate(templateReferenceOf(destination))
                        .setPath(extra.path)
                        .build()
                )
                check(response.result.success) { "Failed to purge '${extra.path}': ${response.result.message}" }
                record = updateRecord(progress.jobId) { current ->
                    current.copy(
                        expectedDestinationInventory = current.expectedDestinationInventory.filterNot { it.path == extra.path },
                        purgedPaths = current.purgedPaths + extra.path,
                        inFlightPurgePath = null,
                        progress = current.progress.copy(
                            filesPurged = current.progress.filesPurged + 1,
                            currentFile = extra.path,
                        ),
                    )
                }
            }
        }

        record = storage.getRecord(progress.jobId) ?: return
        if (record.progress.type == TemplateJobType.MOVE) {
            validateDestinationInventory(record, destination)
            val destinationPaths = record.expectedDestinationInventory.map { it.path }.toSet()
            if (!record.sourceInventory.all { it.path in destinationPaths }) {
                throw unsafeRecovery("destination is missing source files; source was retained")
            }
            record = updateRecord(progress.jobId) { it.copy(phase = TemplateJobPhase.DELETING_SOURCE) }
            val deleted = templateService.deleteTemplate(
                DeleteTemplateRequest.newBuilder()
                    .setTemplate(templateReferenceOf(source))
                    .build()
            )
            check(deleted.result.success) { "Failed to delete source template: ${deleted.result.message}" }
        }

        record = storage.getRecord(progress.jobId) ?: return
        completeJob(record)
    }

    private fun unsafeRecovery(reason: String): IllegalStateException =
        IllegalStateException("manual_recovery_required: $reason; template locks are retained")

    private suspend fun updateRecord(
        jobId: String,
        transform: (TemplateJobRecord) -> TemplateJobRecord,
    ): TemplateJobRecord = storage.updateRecord(jobId) { current ->
        transform(current ?: throw IllegalStateException("Template job $jobId disappeared"))
    } ?: throw IllegalStateException("Failed to persist template job $jobId")

    private suspend fun snapshotFiles(template: Template): List<TemplateFileSnapshot> {
        val entries = mutableListOf<TemplateDirectoryEntry>()
        scanDirectory(template, "", entries)
        return entries.filterNot { it.directory }
            .map { TemplateFileSnapshot(it.path, false, it.size, it.modifiedAt) }
            .sortedBy { it.path }
    }

    private suspend fun snapshotFile(template: Template, path: String): TemplateFileSnapshot? {
        val parent = path.substringBeforeLast('/', "")
        val entries = templateService.listDirectory(
            ListDirectoryRequest.newBuilder()
                .setTemplate(templateReferenceOf(template))
                .setPath(parent)
                .build()
        ).entriesList
        return entries.firstOrNull { it.path == path && !it.directory }
            ?.let { TemplateFileSnapshot(it.path, false, it.size, it.modifiedAt) }
    }

    private suspend fun validateDestinationInventory(record: TemplateJobRecord, destination: Template) {
        val actual = snapshotFiles(destination)
        if (actual != record.expectedDestinationInventory.sortedBy { it.path }) {
            throw unsafeRecovery("destination path/size/modified-time inventory changed")
        }
    }

    private suspend fun validateRecoveryCheckpoint(
        initial: TemplateJobRecord,
        source: Template,
        destination: Template,
    ) {
        var record = initial
        val actualSource = snapshotFiles(source)
        if (record.phase == TemplateJobPhase.DELETING_SOURCE) {
            if (actualSource.any { it !in record.sourceInventory }) {
                throw unsafeRecovery("source contains unexpected files during move finalization")
            }
        } else if (actualSource != record.sourceInventory.sortedBy { it.path }) {
            throw unsafeRecovery("source path/size/modified-time inventory changed")
        }

        val actualDestination = snapshotFiles(destination)
        val expected = record.expectedDestinationInventory.associateBy { it.path }
        val actual = actualDestination.associateBy { it.path }

        if (record.inFlightPurgePath != null) {
            val path = record.inFlightPurgePath
            if (actualDestination.filterNot { it.path == path } != expected.values.filterNot { it.path == path }.sortedBy { it.path }) {
                throw unsafeRecovery("destination changed outside in-flight purge '$path'")
            }
            when {
                path !in actual -> record = updateRecord(record.progress.jobId) {
                    it.copy(
                        expectedDestinationInventory = it.expectedDestinationInventory.filterNot { file -> file.path == path },
                        purgedPaths = it.purgedPaths + path,
                        inFlightPurgePath = null,
                        progress = it.progress.copy(filesPurged = it.progress.filesPurged + 1),
                    )
                }
                actual[path] == expected[path] -> Unit // Delete had not completed; retry it.
                else -> throw unsafeRecovery("in-flight purge file '$path' changed unexpectedly")
            }
            return
        }

        if (record.inFlightPath != null) {
            val path = record.inFlightPath
            if (actualDestination.filterNot { it.path == path } != expected.values.filterNot { it.path == path }.sortedBy { it.path }) {
                throw unsafeRecovery("destination changed outside in-flight file '$path'")
            }
            val actualFile = actual[path]
            val expectedFile = expected[path]
            if (actualFile != expectedFile && actualFile != null) {
                val sourceBytes = templateService.readFile(
                    ReadFileRequest.newBuilder().setTemplate(templateReferenceOf(source)).setPath(path).build()
                ).file.content
                val destinationBytes = templateService.readFile(
                    ReadFileRequest.newBuilder().setTemplate(templateReferenceOf(destination)).setPath(path).build()
                ).file.content
                if (sourceBytes != destinationBytes) throw unsafeRecovery("in-flight file '$path' differs from source")
                val sourceSnapshot = record.sourceInventory.firstOrNull { it.path == path }
                    ?: throw unsafeRecovery("in-flight file '$path' is outside source inventory")
                val overwritten = record.initialDestinationInventory.any { it.path == path }
                record = updateRecord(record.progress.jobId) { current ->
                    current.copy(
                        expectedDestinationInventory = current.expectedDestinationInventory.filterNot { it.path == path } + actualFile,
                        completedPaths = current.completedPaths + path,
                        inFlightPath = null,
                        progress = current.progress.copy(
                            filesProcessed = current.progress.filesProcessed + 1,
                            bytesProcessed = current.progress.bytesProcessed + sourceSnapshot.size,
                            filesCopied = current.progress.filesCopied + if (overwritten) 0 else 1,
                            filesOverwritten = current.progress.filesOverwritten + if (overwritten) 1 else 0,
                        ),
                    )
                }
            } else if (actualFile == expectedFile) {
                record = updateRecord(record.progress.jobId) { it.copy(inFlightPath = null) }
            } else if (actualFile == null) {
                // An interrupted overwrite may have removed the old target before the new file
                // became visible. The source is still locked and unchanged, so retry that write.
                record = updateRecord(record.progress.jobId) {
                    it.copy(
                        expectedDestinationInventory = it.expectedDestinationInventory.filterNot { file -> file.path == path },
                        inFlightPath = null,
                    )
                }
            }
            return
        }

        if (actualDestination != record.expectedDestinationInventory.sortedBy { it.path }) {
            throw unsafeRecovery("destination path/size/modified-time inventory changed")
        }
    }

    private suspend fun completeJob(record: TemplateJobRecord) {
        val completed = updateRecord(record.progress.jobId) { current ->
            current.copy(
                phase = TemplateJobPhase.DELETING_SOURCE,
                progress = current.progress.copy(
                    status = TemplateJobStatus.COMPLETED,
                    completedAt = System.currentTimeMillis(),
                    filesProcessed = current.progress.totalFiles,
                ),
            )
        }
        publishEvent("completed", completed.progress.jobId) { TemplateJobEvents.completed(completed.progress) }
        val keys = lockKeysFor(completed)
        try {
            lockStorage.release(keys, completed.progress.jobId)
            storage.updateRecord(completed.progress.jobId) { current ->
                (current ?: completed).copy(lockKeys = emptySet(), workerLeaseUntil = 0)
            }
        } catch (e: Exception) {
            logger.error("Template job {} completed, but its lock cleanup will be retried", completed.progress.jobId, e)
        }
    }

    private fun lockKeysFor(record: TemplateJobRecord): Set<String> {
        if (record.lockKeys.isNotEmpty()) return record.lockKeys
        val sourceKey = TemplateLockKey.template(record.progress.source.templateId)
        val destinationKeys = if (record.destinationExisted) {
            setOf(TemplateLockKey.template(record.progress.destination.templateId))
        } else {
            setOf(
                TemplateLockKey.template(record.progress.destination.templateId),
                TemplateLockKey.reservation(
                    record.progress.destination.name,
                    record.progress.destination.location.storageName,
                    record.progress.destination.location.nodeName,
                ),
            )
        }
        return destinationKeys + sourceKey
    }

    private suspend fun scanDirectory(
        template: Template,
        path: String,
        result: MutableList<TemplateDirectoryEntry>,
    ) {
        val entries = templateService.listDirectory(
            ListDirectoryRequest.newBuilder()
                .setTemplate(templateReferenceOf(template))
                .setPath(path)
                .build()
        ).entriesList

        for (entry in entries) {
            result.add(entry)
            if (entry.directory) {
                scanDirectory(template, entry.path, result)
            }
        }
    }

    private suspend fun hasFileConflicts(source: Template, destination: Template): Boolean {
        val destFiles = mutableListOf<TemplateDirectoryEntry>()
        scanDirectory(destination, "", destFiles)
        val destPaths = destFiles.filterNot { it.directory }.map { it.path }.toSet()

        val sourceFiles = mutableListOf<TemplateDirectoryEntry>()
        scanDirectory(source, "", sourceFiles)
        val sourcePaths = sourceFiles.filterNot { it.directory }.map { it.path }.toSet()

        return sourcePaths.any { it in destPaths }
    }

    private fun templateReferenceOf(template: Template): TemplateReference =
        TemplateReference.newBuilder()
            .setTemplateId(template.id)
            .setName(template.name)
            .setLocation(template.location.toDefinition())
            .build()
}

data class TemplateJobAcceptResult(
    val status: TemplateJobAcceptStatus,
    val reason: String,
    val job: TemplateJobProgress? = null,
) {
    fun toDefinition(): build.buf.gen.vulpescloud.templates.v1.TemplateJobAcceptResponse {
        val builder = build.buf.gen.vulpescloud.templates.v1.TemplateJobAcceptResponse.newBuilder()
            .setStatus(status.toDefinition())
        if (reason.isNotBlank()) builder.setReason(reason)
        job?.let { builder.setJob(it.toDefinition()) }
        return builder.build()
    }
}
