package de.vulpescloud.node.templates

import build.buf.gen.vulpescloud.templates.v1.CopyTemplateRequest
import build.buf.gen.vulpescloud.templates.v1.CopyTemplateResponse
import build.buf.gen.vulpescloud.templates.v1.CreateDirectoryRequest
import build.buf.gen.vulpescloud.templates.v1.CreateDirectoryResponse
import build.buf.gen.vulpescloud.templates.v1.CreateFileRequest
import build.buf.gen.vulpescloud.templates.v1.CreateFileResponse
import build.buf.gen.vulpescloud.templates.v1.CreateTemplateRequest
import build.buf.gen.vulpescloud.templates.v1.CreateTemplateResponse
import build.buf.gen.vulpescloud.templates.v1.DeleteDirectoryRequest
import build.buf.gen.vulpescloud.templates.v1.DeleteDirectoryResponse
import build.buf.gen.vulpescloud.templates.v1.DeleteFileRequest
import build.buf.gen.vulpescloud.templates.v1.DeleteFileResponse
import build.buf.gen.vulpescloud.templates.v1.DeleteTemplateRequest
import build.buf.gen.vulpescloud.templates.v1.DeleteTemplateResponse
import build.buf.gen.vulpescloud.templates.v1.GetTemplateRequest
import build.buf.gen.vulpescloud.templates.v1.GetTemplateResponse
import build.buf.gen.vulpescloud.templates.v1.ListDirectoryRequest
import build.buf.gen.vulpescloud.templates.v1.ListDirectoryResponse
import build.buf.gen.vulpescloud.templates.v1.ListTemplatesRequest
import build.buf.gen.vulpescloud.templates.v1.ListTemplatesResponse
import build.buf.gen.vulpescloud.templates.v1.MoveTemplateRequest
import build.buf.gen.vulpescloud.templates.v1.MoveTemplateResponse
import build.buf.gen.vulpescloud.templates.v1.ReadFileRequest
import build.buf.gen.vulpescloud.templates.v1.ReadFileResponse
import build.buf.gen.vulpescloud.templates.v1.TemplateDirectoryEntry
import build.buf.gen.vulpescloud.templates.v1.TemplateFile
import build.buf.gen.vulpescloud.templates.v1.TemplateLocation as TemplateLocationDefinition
import build.buf.gen.vulpescloud.templates.v1.TemplateOperationResult
import build.buf.gen.vulpescloud.templates.v1.TemplateReference
import build.buf.gen.vulpescloud.templates.v1.TemplateServiceGrpcKt
import build.buf.gen.vulpescloud.templates.v1.TemplateStorage as TemplateStorageDefinition
import build.buf.gen.vulpescloud.templates.v1.UpdateFileRequest
import build.buf.gen.vulpescloud.templates.v1.UpdateFileResponse
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.protobuf.ByteString
import de.vulpescloud.api.cluster.NodeState
import de.vulpescloud.api.templates.Template
import de.vulpescloud.api.templates.TemplateLocation as ApiTemplateLocation
import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import de.vulpescloud.node.grpc.security.annotations.RequiresPermission
import io.grpc.Status
import io.grpc.StatusException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * Implements the dashboard-facing template CRUD/browsing API.
 *
 * Template *metadata* (id/name/storage/location/version/enabled) lives in the cluster-wide
 * [TemplateRegistry]. The actual file contents live wherever [Template.location] points at:
 * - [de.vulpescloud.api.templates.TemplateStorages.LOCAL]: on disk on the node identified by
 *   `location.nodeId`. If this node isn't that node, the request is forwarded transparently.
 * - any other storage (e.g. S3, provided by an external module): reachable from every node the
 *   same way, so no forwarding is required - see [TemplateStorageProvider].
 *
 * [moveTemplate]/[copyTemplate] are implemented generically on top of the other RPCs
 * (list/read/create/update), which means they transparently support moving/copying a template
 * across nodes and/or across storage backends without either side needing to know about the
 * other's implementation.
 */
class TemplateServiceImpl : TemplateServiceGrpcKt.TemplateServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(TemplateServiceImpl::class.java)

    private val stubCache =
        Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build<String, TemplateServiceGrpcKt.TemplateServiceCoroutineStub>()

    @RequiresPermission("templates.create")
    override suspend fun createTemplate(request: CreateTemplateRequest): CreateTemplateResponse {
        val destination = request.destination

        if (needsForwarding(destination)) {
            val stub = remoteStub(destination.nodeId) ?: throw unavailable(destination.nodeId)
            return stub.createTemplate(request)
        }

        val locationApi = ApiTemplateLocation.fromDefinition(destination)
        val template =
            Template.fromDefinition(request.template)
                .copy(
                    id = request.template.id.ifBlank { UUID.randomUUID().toString() },
                    storage = locationApi.storage,
                    location = locationApi,
                )

        withContext(Dispatchers.IO) { storageFor(template).createTemplate(template) }
        TemplateRegistry.save(template)

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
        if (needsForwarding(location)) {
            val stub =
                remoteStub(location.nodeId)
                    ?: return DeleteTemplateResponse.newBuilder()
                        .setResult(operationResult(false, "Node '${location.nodeId}' is not reachable"))
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
        return GetTemplateResponse.newBuilder().setTemplate(template.toDefinition()).build()
    }

    @RequiresPermission("templates.list")
    override suspend fun listTemplates(request: ListTemplatesRequest): ListTemplatesResponse {
        val templates =
            TemplateRegistry.getAll().filter {
                TemplateRegistry.matchesLocation(it.location.toDefinition(), request.location)
            }
        return ListTemplatesResponse.newBuilder()
            .addAllTemplates(templates.map { it.toDefinition() })
            .build()
    }

    @RequiresPermission("templates.copy")
    override suspend fun copyTemplate(request: CopyTemplateRequest): CopyTemplateResponse {
        val result = performCopy(request.source, request.destination, request.overwrite)
        return CopyTemplateResponse.newBuilder().setResult(result).build()
    }

    @RequiresPermission("templates.move")
    override suspend fun moveTemplate(request: MoveTemplateRequest): MoveTemplateResponse {
        val copyResult = performCopy(request.source, request.destination, request.overwrite)
        if (!copyResult.success) {
            return MoveTemplateResponse.newBuilder().setResult(copyResult).build()
        }

        val deleteResponse =
            deleteTemplate(DeleteTemplateRequest.newBuilder().setTemplate(request.source).build())
        if (!deleteResponse.result.success) {
            return MoveTemplateResponse.newBuilder()
                .setResult(
                    operationResult(
                        false,
                        "Copied but failed to remove source: ${deleteResponse.result.message}",
                    )
                )
                .build()
        }

        return MoveTemplateResponse.newBuilder()
            .setResult(operationResult(true, "Template moved"))
            .build()
    }

    private suspend fun performCopy(
        sourceRef: TemplateReference,
        destinationRef: TemplateReference,
        overwrite: Boolean,
    ): TemplateOperationResult {
        val sourceTemplate =
            TemplateRegistry.findByReference(sourceRef)
                ?: return operationResult(false, "Source template not found")

        val existingDestination = TemplateRegistry.findByReference(destinationRef)
        if (existingDestination != null && !overwrite) {
            return operationResult(false, "Destination template already exists")
        }

        val destinationLocation =
            if (destinationRef.hasLocation()) {
                ApiTemplateLocation.fromDefinition(destinationRef.location)
            } else {
                existingDestination?.location ?: sourceTemplate.location
            }

        val destinationTemplateDraft =
            existingDestination
                ?: sourceTemplate.copy(
                    id = UUID.randomUUID().toString(),
                    name = destinationRef.name.ifBlank { sourceTemplate.name },
                    storage = destinationLocation.storage,
                    location = destinationLocation,
                )

        return try {
            val createResponse =
                createTemplate(
                    CreateTemplateRequest.newBuilder()
                        .setTemplate(destinationTemplateDraft.toDefinition())
                        .setDestination(destinationLocation.toDefinition())
                        .build()
                )
            val destinationTemplate = Template.fromDefinition(createResponse.template)

            copyDirectoryRecursively(sourceTemplate, destinationTemplate, "")

            operationResult(true, "Template copied")
        } catch (e: Exception) {
            logger.error("Failed to copy template '${sourceTemplate.name}'", e)
            operationResult(false, e.message ?: "Unknown error")
        }
    }

    private suspend fun copyDirectoryRecursively(source: Template, destination: Template, path: String) {
        val sourceRef = templateReferenceOf(source)
        val destinationRef = templateReferenceOf(destination)

        val entries =
            listDirectory(
                    ListDirectoryRequest.newBuilder().setTemplate(sourceRef).setPath(path).build()
                )
                .entriesList

        for (entry in entries) {
            if (entry.directory) {
                createDirectory(
                    CreateDirectoryRequest.newBuilder()
                        .setTemplate(destinationRef)
                        .setPath(entry.path)
                        .build()
                )
                copyDirectoryRecursively(source, destination, entry.path)
            } else {
                val file =
                    readFile(
                            ReadFileRequest.newBuilder()
                                .setTemplate(sourceRef)
                                .setPath(entry.path)
                                .build()
                        )
                        .file
                updateFile(
                    UpdateFileRequest.newBuilder()
                        .setTemplate(destinationRef)
                        .setPath(entry.path)
                        .setContent(file.content)
                        .build()
                )
            }
        }
    }

    @RequiresPermission("templates.createDirectory")
    override suspend fun createDirectory(request: CreateDirectoryRequest): CreateDirectoryResponse {
        val template =
            TemplateRegistry.findByReference(request.template)
                ?: return CreateDirectoryResponse.newBuilder()
                    .setResult(operationResult(false, "Template not found"))
                    .build()

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub =
                remoteStub(location.nodeId)
                    ?: return CreateDirectoryResponse.newBuilder()
                        .setResult(operationResult(false, "Node '${location.nodeId}' is not reachable"))
                        .build()
            return stub.createDirectory(request)
        }

        return try {
            withContext(Dispatchers.IO) { storageFor(template).createDirectory(template, request.path) }
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

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub =
                remoteStub(location.nodeId)
                    ?: return DeleteDirectoryResponse.newBuilder()
                        .setResult(operationResult(false, "Node '${location.nodeId}' is not reachable"))
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

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub = remoteStub(location.nodeId) ?: throw unavailable(location.nodeId)
            return stub.listDirectory(request)
        }

        return try {
            val entries =
                withContext(Dispatchers.IO) {
                    storageFor(template).listDirectory(template, request.path)
                }
            ListDirectoryResponse.newBuilder().addAllEntries(entries.map { toProtoEntry(it) }).build()
        } catch (e: IllegalArgumentException) {
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

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub =
                remoteStub(location.nodeId)
                    ?: return CreateFileResponse.newBuilder()
                        .setResult(operationResult(false, "Node '${location.nodeId}' is not reachable"))
                        .build()
            return stub.createFile(request)
        }

        return try {
            withContext(Dispatchers.IO) {
                storageFor(template).createFile(template, request.path, request.content.toByteArray())
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

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub =
                remoteStub(location.nodeId)
                    ?: return UpdateFileResponse.newBuilder()
                        .setResult(operationResult(false, "Node '${location.nodeId}' is not reachable"))
                        .build()
            return stub.updateFile(request)
        }

        return try {
            withContext(Dispatchers.IO) {
                storageFor(template).updateFile(template, request.path, request.content.toByteArray())
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

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub =
                remoteStub(location.nodeId)
                    ?: return DeleteFileResponse.newBuilder()
                        .setResult(operationResult(false, "Node '${location.nodeId}' is not reachable"))
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

        val location = template.location.toDefinition()
        if (needsForwarding(location)) {
            val stub = remoteStub(location.nodeId) ?: throw unavailable(location.nodeId)
            return stub.readFile(request)
        }

        return try {
            val file =
                withContext(Dispatchers.IO) { storageFor(template).readFile(template, request.path) }
            ReadFileResponse.newBuilder().setFile(toProtoFile(file)).build()
        } catch (e: IllegalArgumentException) {
            throw StatusException(Status.NOT_FOUND.withDescription(e.message))
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription(e.message))
        }
    }


    private fun storageFor(template: Template): TemplateStorage =
        Node.instance.templateStorageProvider.getTemplateStorage(template.storage)

    private fun needsForwarding(location: TemplateLocationDefinition): Boolean =
        location.storage == TemplateStorageDefinition.TEMPLATE_STORAGE_LOCAL &&
            location.nodeId.isNotBlank() &&
            location.nodeId != Node.instance.configProvider.config.nodeName

    private suspend fun remoteStub(
        nodeId: String
    ): TemplateServiceGrpcKt.TemplateServiceCoroutineStub? {
        val remoteNode =
            Node.instance.clusterProvider.remoteNodes.find { it.endpoint.name == nodeId }
                ?: return null
        if (remoteNode.getSnapshot().state != NodeState.ONLINE) return null
        return stubCache.get(nodeId) { _ ->
            TemplateServiceGrpcKt.TemplateServiceCoroutineStub(remoteNode.channel!!)
                .withInterceptors(AuthClientInterceptor(Node.instance.secret))
        }
    }

    private fun unavailable(nodeId: String): StatusException =
        StatusException(Status.UNAVAILABLE.withDescription("Node '$nodeId' is not reachable"))

    private fun operationResult(success: Boolean, message: String): TemplateOperationResult =
        TemplateOperationResult.newBuilder().setSuccess(success).setMessage(message).build()

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
