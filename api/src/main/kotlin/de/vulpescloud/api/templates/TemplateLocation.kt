package de.vulpescloud.api.templates

import build.buf.gen.vulpescloud.templates.v1.TemplateLocation as TemplateLocationDefinition
import build.buf.gen.vulpescloud.templates.v1.TemplateStorage as TemplateStorageDefinition
import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.BsonString

/**
 * Where a [Template] physically lives.
 *
 * - For [TemplateStorages.LOCAL] the template is stored on disk on the node identified by
 *   [nodeId]. Any RPC touching the actual file contents has to be executed on that node, so
 *   callers need to route/forward accordingly.
 * - For any other storage (e.g. an S3-backed storage provided by an external module) [nodeId]
 *   is irrelevant, as the backing storage is expected to be reachable from every node the same
 *   way. [storageName] identifies which configured storage backend (e.g. bucket/profile name)
 *   should be used.
 */
@Serializable
data class TemplateLocation(
    val storage: TemplateStorages = TemplateStorages.LOCAL,
    val nodeId: String = "",
    val storageName: String = "",
    val path: String = "",
) {

    fun toDocument(): BsonDocument =
        BsonDocument().apply {
            append("storage", BsonString(storage.name))
            append("nodeId", BsonString(nodeId))
            append("storageName", BsonString(storageName))
            append("path", BsonString(path))
        }

    fun toDefinition(): TemplateLocationDefinition {
        val builder =
            TemplateLocationDefinition.newBuilder()
                .setNodeId(nodeId)
                .setStorageName(storageName)
                .setPath(path)

        when (storage) {
            TemplateStorages.LOCAL ->
                builder.setStorage(TemplateStorageDefinition.TEMPLATE_STORAGE_LOCAL)
            TemplateStorages.S3 -> builder.setStorage(TemplateStorageDefinition.TEMPLATE_STORAGE_S3)
        }

        return builder.build()
    }

    companion object {
        fun fromDefinition(definition: TemplateLocationDefinition): TemplateLocation {
            return TemplateLocation(
                when (definition.storage) {
                    TemplateStorageDefinition.TEMPLATE_STORAGE_LOCAL -> TemplateStorages.LOCAL
                    TemplateStorageDefinition.TEMPLATE_STORAGE_S3 -> TemplateStorages.S3
                    else -> TemplateStorages.LOCAL
                },
                definition.nodeId,
                definition.storageName,
                definition.path,
            )
        }

        fun fromDocument(document: BsonDocument): TemplateLocation =
            TemplateLocation(
                TemplateStorages.valueOf(document.getString("storage").value),
                document.getString("nodeId").value,
                document.getString("storageName").value,
                document.getString("path").value,
            )
    }
}
