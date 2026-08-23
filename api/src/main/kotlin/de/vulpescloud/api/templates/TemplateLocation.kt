package de.vulpescloud.api.templates

import build.buf.gen.vulpescloud.templates.v1.TemplateLocation as ProtoTemplateLocation
import kotlinx.serialization.Serializable

@Serializable
data class TemplateLocation(
    val storageName: String,
    val nodeName: String?,
) {
    fun toDefinition(): ProtoTemplateLocation =
        ProtoTemplateLocation.newBuilder()
            .setStorageName(storageName)
            .apply {
                this@TemplateLocation.nodeName?.let { setNodeName(it) }
            }
            .build()

    companion object {
        fun fromDefinition(definition: ProtoTemplateLocation): TemplateLocation {
            return TemplateLocation(
                storageName = definition.storageName,
                nodeName = definition.nodeName,
            )
        }
    }
}
