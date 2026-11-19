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

import build.buf.gen.vulpescloud.templates.v1.*
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.cluster.NodeState
import org.vulpesstudios.vulpescloud.api.templates.*
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.grpc.security.AuthClientInterceptor
import org.vulpesstudios.vulpescloud.node.grpc.security.annotations.RequiresPermission
import java.util.*
import java.util.concurrent.TimeUnit
import build.buf.gen.vulpescloud.templates.v1.TemplateJobProgress as ProtoTemplateJobProgress
import build.buf.gen.vulpescloud.templates.v1.TemplateLocation as TemplateLocationDefinition
import build.buf.gen.vulpescloud.templates.v1.TemplateStorage as TemplateStorageDefinition
import org.vulpesstudios.vulpescloud.api.templates.TemplateLocation as ApiTemplateLocation

class TemplateServiceImpl : TemplateServiceGrpcKt.TemplateServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(TemplateServiceImpl::class.java)

    private val jobStorage = TemplateJobStorage()
    private val jobLockStorage = TemplateJobLockStorage()
    private val jobEngine = TemplateJobEngine(jobStorage).also {
        it.setTemplateService { this }
    }
    private var jobRecoveryLoop: Job? = null

    private val stubCache =
        Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build<String, TemplateServiceGrpcKt.TemplateServiceCoroutineStub>()

    fun startJobRecoveryLoop() {
        if (jobRecoveryLoop?.isActive == true) return
        jobRecoveryLoop = org.vulpesstudios.vulpescloud.node.NodeCoroutineScope.launch {
            while (isActive) {
                try {
                    jobEngine.reconcile()
                } catch (e: Exception) {
                    logger.error("Template job reconciliation failed", e)
                }
                delay(2_000)
            }
        }
    }

    @RequiresPermission("templates.create")
    override suspend fun createTemplate(request: CreateTemplateRequest): CreateTemplateResponse {
        // Proto3 returns an empty string for an omitted nodeName.  Persisting that value makes
        // node-local storages (such as LOCAL) look cluster-wide and loses the node affinity on
        // every create/upsert. Resolve it before both forwarding and persistence.
        val destination = resolveDestination(request.destination)

        val requestedId = request.template.id
        val templateId = requestedId.ifBlank { Template.normalizedId(request.template.name) }
        if (!Template.isValidId(templateId)) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription(
                "Template id '$templateId' is invalid; use letters, numbers, dots, underscores, and hyphens"
            ))
        }
        if (templateId.isBlank()) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription(
                "Template name must contain at least one letter or number to generate an id"
            ))
        }
        val templateLock = templateId.takeIf { it.isNotBlank() }
            ?.let { jobLockStorage.get(TemplateLockKey.template(it)) }
        val activeLock = templateLock?.takeUnless { canUseJobLock(it) }
        if (activeLock != null) throw templateLocked(activeLock)

        if (needsForwarding(destination)) {
            val stub = remoteStub(destination.nodeName) ?: throw unavailable(destination.nodeName)
            val forwarded = request.toBuilder()
                .setTemplate(request.template.toBuilder().setId(templateId).build())
                .setDestination(destination)
                .build()
            return stub.createTemplate(forwarded)
        }

        val locationApi = ApiTemplateLocation.fromDefinition(destination)
        val template =
            Template.fromDefinition(request.template)
                .copy(
                    id = templateId,
                    location = locationApi,
                )

        if (!TemplateRegistry.create(template)) {
            throw StatusException(Status.ALREADY_EXISTS.withDescription(
                "Template id '$templateId' is already in use"
            ))
        }
        try {
            withContext(Dispatchers.IO) { storageFor(template).createTemplate(template) }
        } catch (e: Exception) {
            TemplateRegistry.delete(template)
            throw e
        }

        return CreateTemplateResponse.newBuilder().setTemplate(template.toDefinition()).build()
    }

    @RequiresPermission("templates.delete")
    override suspend fun deleteTemplate(request: DeleteTemplateRequest): DeleteTemplateResponse {
        val template =
            TemplateRegistry.findByReference(request.template)
                ?: return DeleteTemplateResponse.newBuilder()
                    .setResult(operationResult(false, "Template not found"))
                    .build()

        val location = template.location.toDefinition()
        lockedResponse(template)?.let { return DeleteTemplateResponse.newBuilder().setResult(it).build() }
        if (needsForwarding(location)) {
            val stub =
                remoteStub(location.nodeName)
                    ?: return DeleteTemplateResponse.newBuilder()
                        .setResult(
                            operationResult(false, "Node '${location.nodeName}' is not reachable")
                        )
                        .build()
            return stub.deleteTemplate(request)
        }

        return try {
            withContext(Dispatchers.IO) { storageFor(template).deleteTemplate(template) }
            TemplateRegistry.delete(template)
            DeleteTemplateResponse.newBuilder()
                .setResult(operationResult(true, "Template deleted"))
                .build()
        } catch (e: Exception) {
            logger.error("Failed to delete template '${template.name}'", e)
            DeleteTemplateResponse.newBuilder()
                .setResult(operationResult(false, e.message ?: "Unknown error"))
                .build()
        }
    }

    @RequiresPermission("templates.get")
    override suspend fun getTemplate(request: GetTemplateRequest): GetTemplateResponse {
        val template =
            TemplateRegistry.findByReference(request.template)
                ?: throw StatusException(Status.NOT_FOUND.withDescription("Template not found"))
        ensureUnlocked(template)
        return GetTemplateResponse.newBuilder().setTemplate(template.toDefinition()).build()
    }

    @RequiresPermission("templates.list")
    override suspend fun listTemplates(request: ListTemplatesRequest): ListTemplatesResponse {
        val templates = mutableListOf<Template>()
        for (template in TemplateRegistry.getAll()) {
            if (!TemplateRegistry.matchesLocation(template.location.toDefinition(), request.location)) continue
            if (jobLockStorage.get(TemplateLockKey.template(template.id)) == null) templates += template
        }

        val response = ListTemplatesResponse.newBuilder()
            .addAllTemplates(templates.map { it.toDefinition() })
            .build()
        return response
    }

    @RequiresPermission("templates.copy")
    override suspend fun copyTemplate(request: CopyTemplateRequest): CopyTemplateResponse {
        val conflictMode = TemplateJobConflictMode.fromDefinition(request.conflictMode)
        val extraFilesMode = TemplateJobExtraFilesMode.fromDefinition(request.extraFilesMode)

        val response = jobEngine.validateAndAccept(
            jobType = TemplateJobType.COPY,
            sourceRef = request.source,
            destinationRef = request.destination,
            conflictMode = conflictMode,
            extraFilesMode = extraFilesMode,
        )

        if (response.status == TemplateJobAcceptStatus.ACCEPTED && response.job != null) {
            jobEngine.execute(response.job)
        }

        return CopyTemplateResponse.newBuilder()
            .setAcceptResult(response.toDefinition())
            .build()
    }

    @RequiresPermission("templates.move")
    override suspend fun moveTemplate(request: MoveTemplateRequest): MoveTemplateResponse {
        val conflictMode = TemplateJobConflictMode.fromDefinition(request.conflictMode)
        val extraFilesMode = TemplateJobExtraFilesMode.fromDefinition(request.extraFilesMode)

        val response = jobEngine.validateAndAccept(
            jobType = TemplateJobType.MOVE,
            sourceRef = request.source,
            destinationRef = request.destination,
            conflictMode = conflictMode,
            extraFilesMode = extraFilesMode,
        )

        if (response.status == TemplateJobAcceptStatus.ACCEPTED && response.job != null) {
            jobEngine.execute(response.job)
        }

        return MoveTemplateResponse.newBuilder()
            .setAcceptResult(response.toDefinition())
            .build()
    }

    @RequiresPermission("templates.job.status")
    override suspend fun getTemplateJob(request: GetTemplateJobRequest): ProtoTemplateJobProgress {
        jobStorage.get(request.jobId)?.let { progress ->
            return progress.toDefinition()
        }
        throw StatusException(Status.NOT_FOUND.withDescription("Job not found: ${request.jobId}"))
    }

    @RequiresPermission("templates.job.list")
    override suspend fun listActiveTemplateJobs(request: Empty): ListActiveTemplateJobsResponse {
        val jobs = jobStorage.getActive().map { it.toDefinition() }
        return ListActiveTemplateJobsResponse.newBuilder().addAllJobs(jobs).build()
    }

    internal suspend fun listAllTemplateJobs(): List<ProtoTemplateJobProgress> =
        jobStorage.getAllRecords().map { it.progress.toDefinition() }

    @RequiresPermission("templates.job.cancel")
    override suspend fun cancelTemplateJob(request: CancelTemplateJobRequest): CancelTemplateJobResponse {
        val record = jobStorage.getRecord(request.jobId)
            ?: return CancelTemplateJobResponse.newBuilder().setSuccess(false).setMessage("Job not found: ${request.jobId}").build()
        if (!record.progress.status.isActive) {
            return CancelTemplateJobResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Job is already finished; use 'template job unlock ${request.jobId}' to release a manual lock")
                .build()
        }

        val localNode = Node.instance.configProvider.config.nodeName
        val workerNode = record.workerNodeName
        if (workerNode.isNullOrBlank()) {
            val success = jobEngine.cancelUnclaimed(request.jobId)
            return CancelTemplateJobResponse.newBuilder()
                .setSuccess(success)
                .setMessage(if (success) "Job cancelled before a worker claimed it" else "Job worker has an active lease")
                .build()
        }
        if (workerNode == localNode) {
            val success = jobEngine.cancel(request.jobId)
            return CancelTemplateJobResponse.newBuilder()
                .setSuccess(success)
                .setMessage(if (success) "Job cancellation requested" else "Job worker is not active")
                .build()
        }

        val stub = remoteStub(workerNode)
            ?: return CancelTemplateJobResponse.newBuilder().setSuccess(false).setMessage("Job owner '$workerNode' is not reachable").build()
        return stub.cancelTemplateJob(request)
    }

    suspend fun unlockTemplateJobManually(jobId: String): Boolean =
        jobEngine.unlockForManualRecovery(jobId)

    @RequiresPermission("templates.job.stream")
    override fun streamTemplateJobProgress(request: GetTemplateJobRequest): Flow<ProtoTemplateJobProgress> = callbackFlow {
        // All nodes use the shared job database, so progress can be streamed locally.
        val localJob = jobStorage.get(request.jobId)
        if (localJob != null) {
            // Stream from local job
            suspend fun sendLatest() {
                val value = jobStorage.get(request.jobId) ?: return
                trySend(value.toDefinition())
                if (!value.status.isActive) close()
            }
            sendLatest()
            val job = launch {
                while (true) {
                    delay(500)
                    sendLatest()
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        logger.debug("Job {} not found in shared job storage", request.jobId)
        close(StatusException(Status.NOT_FOUND.withDescription("Job not found: ${request.jobId}")))
    }


    @RequiresPermission("templates.createDirectory")
    override suspend fun createDirectory(request: CreateDirectoryRequest): CreateDirectoryResponse {
        val template =
            TemplateRegistry.findByReference(request.template)
                ?: return CreateDirectoryResponse.newBuilder()
                    .setResult(operationResult(false, "Template not found"))
                    .build()
        lockedResponse(template)?.let { return CreateDirectoryResponse.newBuilder().setResult(it).build() }

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub =
                remoteStub(location.nodeName)
                    ?: return CreateDirectoryResponse.newBuilder()
                        .setResult(
                            operationResult(false, "Node '${location.nodeName}' is not reachable")
                        )
                        .build()
            return stub.createDirectory(request)
        }

        return try {
            withContext(Dispatchers.IO) {
                storageFor(template).createDirectory(template, request.path)
            }
            CreateDirectoryResponse.newBuilder()
                .setResult(operationResult(true, "Directory created"))
                .build()
        } catch (e: Exception) {
            CreateDirectoryResponse.newBuilder()
                .setResult(operationResult(false, e.message ?: "Unknown error"))
                .build()
        }
    }

    @RequiresPermission("templates.deleteDirectory")
    override suspend fun deleteDirectory(request: DeleteDirectoryRequest): DeleteDirectoryResponse {
        val template =
            TemplateRegistry.findByReference(request.template)
                ?: return DeleteDirectoryResponse.newBuilder()
                    .setResult(operationResult(false, "Template not found"))
                    .build()
        lockedResponse(template)?.let { return DeleteDirectoryResponse.newBuilder().setResult(it).build() }

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub =
                remoteStub(location.nodeName)
                    ?: return DeleteDirectoryResponse.newBuilder()
                        .setResult(
                            operationResult(false, "Node '${location.nodeName}' is not reachable")
                        )
                        .build()
            return stub.deleteDirectory(request)
        }

        return try {
            withContext(Dispatchers.IO) {
                storageFor(template).deleteDirectory(template, request.path, request.recursive)
            }
            DeleteDirectoryResponse.newBuilder()
                .setResult(operationResult(true, "Directory deleted"))
                .build()
        } catch (e: Exception) {
            DeleteDirectoryResponse.newBuilder()
                .setResult(operationResult(false, e.message ?: "Unknown error"))
                .build()
        }
    }

    @RequiresPermission("templates.listDirectory")
    override suspend fun listDirectory(request: ListDirectoryRequest): ListDirectoryResponse {
        val template =
            TemplateRegistry.findByReference(request.template)
                ?: throw StatusException(Status.NOT_FOUND.withDescription("Template not found"))
        ensureUnlocked(template)

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub = remoteStub(location.nodeName) ?: throw unavailable(location.nodeName)
            return stub.listDirectory(request)
        }

        return try {
            val entries =
                withContext(Dispatchers.IO) {
                    storageFor(template).listDirectory(template, request.path)
                }
            ListDirectoryResponse.newBuilder()
                .addAllEntries(entries.map { toProtoEntry(it) })
                .build()
        } catch (e: IllegalArgumentException) {
            logger.error(
                "Failed to list template '${template.name}' at path '${request.path}'",
                e,
            )
            e.printStackTrace()
            throw StatusException(Status.INVALID_ARGUMENT.withDescription(e.message))
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription(e.message))
        }
    }

    @RequiresPermission("templates.createFile")
    override suspend fun createFile(request: CreateFileRequest): CreateFileResponse {
        val template =
            TemplateRegistry.findByReference(request.template)
                ?: return CreateFileResponse.newBuilder()
                    .setResult(operationResult(false, "Template not found"))
                    .build()
        lockedResponse(template)?.let { return CreateFileResponse.newBuilder().setResult(it).build() }

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub =
                remoteStub(location.nodeName)
                    ?: return CreateFileResponse.newBuilder()
                        .setResult(
                            operationResult(false, "Node '${location.nodeName}' is not reachable")
                        )
                        .build()
            return stub.createFile(request)
        }

        return try {
            withContext(Dispatchers.IO) {
                storageFor(template)
                    .createFile(template, request.path, request.content.toByteArray())
            }
            CreateFileResponse.newBuilder().setResult(operationResult(true, "File created")).build()
        } catch (e: Exception) {
            CreateFileResponse.newBuilder()
                .setResult(operationResult(false, e.message ?: "Unknown error"))
                .build()
        }
    }

    @RequiresPermission("templates.updateFile")
    override suspend fun updateFile(request: UpdateFileRequest): UpdateFileResponse {
        val template =
            TemplateRegistry.findByReference(request.template)
                ?: return UpdateFileResponse.newBuilder()
                    .setResult(operationResult(false, "Template not found"))
                    .build()
        lockedResponse(template)?.let { return UpdateFileResponse.newBuilder().setResult(it).build() }

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub =
                remoteStub(location.nodeName)
                    ?: return UpdateFileResponse.newBuilder()
                        .setResult(
                            operationResult(false, "Node '${location.nodeName}' is not reachable")
                        )
                        .build()
            return stub.updateFile(request)
        }

        return try {
            withContext(Dispatchers.IO) {
                storageFor(template)
                    .updateFile(template, request.path, request.content.toByteArray())
            }
            UpdateFileResponse.newBuilder().setResult(operationResult(true, "File updated")).build()
        } catch (e: Exception) {
            UpdateFileResponse.newBuilder()
                .setResult(operationResult(false, e.message ?: "Unknown error"))
                .build()
        }
    }

    @RequiresPermission("templates.deleteFile")
    override suspend fun deleteFile(request: DeleteFileRequest): DeleteFileResponse {
        val template =
            TemplateRegistry.findByReference(request.template)
                ?: return DeleteFileResponse.newBuilder()
                    .setResult(operationResult(false, "Template not found"))
                    .build()
        lockedResponse(template)?.let { return DeleteFileResponse.newBuilder().setResult(it).build() }

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub =
                remoteStub(location.nodeName)
                    ?: return DeleteFileResponse.newBuilder()
                        .setResult(
                            operationResult(false, "Node '${location.nodeName}' is not reachable")
                        )
                        .build()
            return stub.deleteFile(request)
        }

        return try {
            withContext(Dispatchers.IO) { storageFor(template).deleteFile(template, request.path) }
            DeleteFileResponse.newBuilder().setResult(operationResult(true, "File deleted")).build()
        } catch (e: Exception) {
            DeleteFileResponse.newBuilder()
                .setResult(operationResult(false, e.message ?: "Unknown error"))
                .build()
        }
    }

    @RequiresPermission("templates.readFile")
    override suspend fun readFile(request: ReadFileRequest): ReadFileResponse {
        val template =
            TemplateRegistry.findByReference(request.template)
                ?: throw StatusException(Status.NOT_FOUND.withDescription("Template not found"))
        ensureUnlocked(template)

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub = remoteStub(location.nodeName) ?: throw unavailable(location.nodeName)
            return stub.readFile(request)
        }

        return try {
            val file =
                withContext(Dispatchers.IO) {
                    storageFor(template).readFile(template, request.path)
                }
            ReadFileResponse.newBuilder().setFile(toProtoFile(file)).build()
        } catch (e: IllegalArgumentException) {
            throw StatusException(Status.NOT_FOUND.withDescription(e.message))
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription(e.message))
        }
    }

    @RequiresPermission("templates.listRegisteredStorages")
    override suspend fun listRegisteredStorages(
        request: ListRegisteredStoragesRequest
    ): ListRegisteredStoragesResponse {
        val type = request.typeOrNull
        val storages: MutableList<TemplateStorageDefinition> =
            TemplateStorageRegistry.getAllTemplateStorages(type)
                .map { storage ->
                    TemplateStorageDefinition.newBuilder()
                        .setType(storage.type())
                        .setName(storage.name())
                        .apply { if (storage.nodeName() != null) setNodeName(storage.nodeName()) }
                        .build()
                }
                .toMutableList()

        Node.instance.clusterProvider.remoteNodes.forEach { node ->
            val stub = remoteStub(node.endpoint.name) ?: return@forEach
            storages.addAll(
                stub
                    .listLocalRegisteredStorages(
                        listLocalRegisteredStoragesRequest { if (type != null) this.type = request.type }
                    )
                    .storageList
                    .filter { storage -> storage.nodeName == node.endpoint.name }
            )
        }
        return ListRegisteredStoragesResponse.newBuilder().addAllStorage(storages).build()
    }

    override suspend fun listLocalRegisteredStorages(
        request: ListLocalRegisteredStoragesRequest
    ): ListLocalRegisteredStoragesResponse {
        val type = request.typeOrNull
        val storages: MutableList<TemplateStorageDefinition> =
            TemplateStorageRegistry.getAllTemplateStorages(type)
                .map { storage ->
                    TemplateStorageDefinition.newBuilder()
                        .setType(storage.type())
                        .setName(storage.name())
                        .apply { if (storage.nodeName() != null) setNodeName(storage.nodeName()) }
                        .build()
                }
                .toMutableList()
        return ListLocalRegisteredStoragesResponse.newBuilder().addAllStorage(storages).build()
    }

    private fun storageFor(template: Template): TemplateStorage =
        TemplateStorageRegistry.getTemplateStorageByName(template.location.storageName)
            ?: throw IllegalArgumentException("Template not found")

    private suspend fun resolveDestination(
        location: TemplateLocationDefinition
    ): TemplateLocationDefinition {
        val resolved = resolveLocation(location.storageName, location.nodeName.takeIf { it.isNotBlank() })
        return location.toBuilder().setNodeName(resolved.nodeName.orEmpty()).build()
    }

    /** Resolves a node-affine storage when a caller selected only its storage name. */
    internal suspend fun resolveLocation(storageName: String, requestedNode: String?): ApiTemplateLocation {
        if (!requestedNode.isNullOrBlank()) return ApiTemplateLocation(storageName, requestedNode)
        val localStorage = TemplateStorageRegistry.getTemplateStorageByName(storageName)
        if (localStorage != null && localStorage.nodeName().isNullOrBlank()) {
            return ApiTemplateLocation(storageName, null)
        }
        val candidates = mutableSetOf<String>()
        localStorage?.nodeName()?.takeIf { it.isNotBlank() }?.let(candidates::add)
        for (node in Node.instance.clusterProvider.remoteNodes) {
            val stub = remoteStub(node.endpoint.name) ?: continue
            val registered = runCatching {
                stub.listLocalRegisteredStorages(listLocalRegisteredStoragesRequest {}).storageList
            }.getOrDefault(emptyList())
            if (registered.any { it.name == storageName }) candidates += node.endpoint.name
        }
        if (candidates.size > 1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription(
                "Storage '$storageName' exists on multiple nodes; specify it as id@$storageName@<node>"
            ))
        }
        return ApiTemplateLocation(storageName, candidates.singleOrNull())
    }

    private fun needsForwarding(location: TemplateLocationDefinition): Boolean =
        location.nodeName.isNotBlank() &&
            location.nodeName != Node.instance.configProvider.config.nodeName

    private suspend fun remoteStub(
        nodeId: String
    ): TemplateServiceGrpcKt.TemplateServiceCoroutineStub? {
        if (nodeId == Node.instance.configProvider.config.nodeName) {return null}
        val remoteNode =
            Node.instance.clusterProvider.remoteNodes.find { it.endpoint.name == nodeId }
                ?: run {
                    return null
                }
        if (remoteNode.getSnapshot().state != NodeState.ONLINE) {
            return null
        }
        val stub = stubCache.get(nodeId) { _ ->
            TemplateServiceGrpcKt.TemplateServiceCoroutineStub(remoteNode.channel!!)
                .withInterceptors(AuthClientInterceptor(Node.instance.secret))
        }
        val identity = TemplateJobCallContext.currentIdentity()
        return if (identity == null) stub else stub.withInterceptors(TemplateJobCallContext.clientInterceptor(identity))
    }

    internal suspend fun remoteStubForRecovery(nodeId: String): TemplateServiceGrpcKt.TemplateServiceCoroutineStub? =
        remoteStub(nodeId)

    private fun unavailable(nodeId: String): StatusException =
        StatusException(Status.UNAVAILABLE.withDescription("Node '$nodeId' is not reachable"))

    private fun operationResult(success: Boolean, message: String): TemplateOperationResult =
        TemplateOperationResult.newBuilder().setSuccess(success).setMessage(message).build()

    private suspend fun lockedResponse(template: Template): TemplateOperationResult? {
        val lock = jobLockStorage.get(TemplateLockKey.template(template.id)) ?: return null
        if (canUseJobLock(lock)) return null
        return operationResult(false, "Template is locked by job ${lock.jobId}: ${lock.description}")
    }

    private suspend fun ensureUnlocked(template: Template) {
        val lock = jobLockStorage.get(TemplateLockKey.template(template.id)) ?: return
        if (!canUseJobLock(lock)) throw templateLocked(lock)
    }

    private suspend fun canUseJobLock(lock: TemplateJobLock): Boolean {
        val identity = TemplateJobCallContext.currentIdentity() ?: return false
        if (identity.jobId != lock.jobId) return false
        return jobStorage.getRecord(identity.jobId)?.workerToken == identity.token
    }

    private fun templateLocked(lock: TemplateJobLock): StatusException =
        StatusException(
            Status.FAILED_PRECONDITION.withDescription(
                "Template is locked by job ${lock.jobId}: ${lock.description}"
            )
        )

    private fun templateReferenceOf(template: Template): TemplateReference =
        TemplateReference.newBuilder()
            .setTemplateId(template.id)
            .setName(template.name)
            .setLocation(template.location.toDefinition())
            .build()

    private fun toProtoFile(data: TemplateFileData): TemplateFile =
        TemplateFile.newBuilder()
            .setPath(data.path)
            .setContent(ByteString.copyFrom(data.content))
            .setSize(data.size)
            .setMimeType(data.mimeType)
            .setModifiedAt(data.modifiedAt)
            .build()

    private fun toProtoEntry(data: TemplateDirectoryEntryData): TemplateDirectoryEntry =
        TemplateDirectoryEntry.newBuilder()
            .setName(data.name)
            .setPath(data.path)
            .setDirectory(data.directory)
            .setSize(data.size)
            .setModifiedAt(data.modifiedAt)
            .build()
}
