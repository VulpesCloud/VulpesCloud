package de.vulpescloud.api.templates

import build.buf.gen.vulpescloud.templates.v1.TemplateDefinition
import build.buf.gen.vulpescloud.templates.v1.TemplateStorage
import kotlinx.serialization.Serializable
import org.bson.BsonBoolean
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString

@Serializable
data class Template(
    val name: String,
    val storage: TemplateStorages,
    val weight: Int,
    val id: String = "",
    val location: TemplateLocation = TemplateLocation(storage = storage),
    val version: String = "",
    val enabled: Boolean = true,
) {

    fun toDocument(): BsonDocument =
        BsonDocument().apply {
            append("id", BsonString(id))
            append("name", BsonString(name))
            append("storage", BsonInt32(storage.ordinal))
            append("weight", BsonInt32(weight))
            append("location", location.toDocument())
            append("version", BsonString(version))
            append("enabled", BsonBoolean(enabled))
        }

    fun toDefinition(): TemplateDefinition {
        val builder =
            TemplateDefinition.newBuilder()
                .setId(id)
                .setName(name)
                .setWeight(weight)
                .setLocation(location.toDefinition())
                .setVersion(version)
                .setEnabled(enabled)

        when (storage) {
            TemplateStorages.LOCAL ->
                (builder.setStorage(TemplateStorage.TEMPLATE_STORAGE_LOCAL))
            TemplateStorages.S3 ->
                (builder.setStorage(TemplateStorage.TEMPLATE_STORAGE_S3))
        }

        return builder.build()
    }

    companion object {
        fun fromDefinition(templateDefinition: TemplateDefinition): Template {
            val storage =
                when (templateDefinition.storage) {
                    TemplateStorage.TEMPLATE_STORAGE_LOCAL ->
                        TemplateStorages.LOCAL
                    TemplateStorage.TEMPLATE_STORAGE_S3 ->
                        TemplateStorages.S3
                    else -> TemplateStorages.LOCAL
                }

            return Template(
                templateDefinition.name,
                storage,
                templateDefinition.weight,
                templateDefinition.id,
                if (templateDefinition.hasLocation())
                    TemplateLocation.fromDefinition(templateDefinition.location)
                else TemplateLocation(storage = storage),
                templateDefinition.version,
                templateDefinition.enabled,
            )
        }

        fun fromDocument(document: BsonDocument): Template {
            val storage =
                TemplateStorages.entries[
                        document.getInt32("storage").value]

            return Template(
                document.getString("name").value,
                storage,
                document.getInt32("weight").value,
                document.getString("id", BsonString("")).value,
                document.getDocument("location", null)?.let { TemplateLocation.fromDocument(it) }
                    ?: TemplateLocation(storage = storage),
                document.getString("version", BsonString("")).value,
                document.getBoolean("enabled", BsonBoolean(true)).value,
            )
        }
    }
}
