package de.vulpescloud.api.templates

import build.buf.gen.vulpescloud.tasks.v1.TemplateDefinition
import build.buf.gen.vulpescloud.tasks.v1.TemplateStorage
import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString

@Serializable
data class Template(
    val name: String,
    val storage: de.vulpescloud.api.templates.TemplateStorages,
    val weight: Int,
) {

    fun toDocument(): BsonDocument =
        BsonDocument().apply {
            append("name", BsonString(name))
            append("storage", BsonInt32(storage.ordinal))
            append("weight", BsonInt32(weight))
        }

    fun toDefinition(): TemplateDefinition {
        val builder = TemplateDefinition.newBuilder().setName(name).setWeight(weight)

        when (storage) {
            de.vulpescloud.api.templates.TemplateStorages.LOCAL ->
                (builder.setStorage(TemplateStorage.TEMPLATE_STORAGE_LOCAL_UNSPECIFIED))
            de.vulpescloud.api.templates.TemplateStorages.S3 ->
                (builder.setStorage(TemplateStorage.TEMPLATE_STORAGE_S3))
        }

        return builder.build()
    }

    companion object {
        fun fromDefinition(templateDefinition: TemplateDefinition): Template {
            return Template(
                templateDefinition.name,
                when (templateDefinition.storage) {
                    TemplateStorage.TEMPLATE_STORAGE_LOCAL_UNSPECIFIED ->
                        de.vulpescloud.api.templates.TemplateStorages.LOCAL
                    TemplateStorage.TEMPLATE_STORAGE_S3 ->
                        de.vulpescloud.api.templates.TemplateStorages.S3
                    else -> de.vulpescloud.api.templates.TemplateStorages.LOCAL
                },
                templateDefinition.weight,
            )
        }

        fun fromDocument(document: BsonDocument): Template =
            Template(
                document.getString("name").value,
                de.vulpescloud.api.templates.TemplateStorages.entries[
                        document.getInt32("storage").value],
                document.getInt32("weight").value,
            )
    }
}
