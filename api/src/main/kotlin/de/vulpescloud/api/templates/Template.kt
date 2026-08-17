package de.vulpescloud.api.templates

import build.buf.gen.vulpescloud.templates.v1.TemplateDefinition
import kotlinx.serialization.Serializable

@Serializable
data class Template(
    val name: String,
    val weight: Int,
    val id: String = "",
    val location: TemplateLocation,
    val version: String = "",
    val enabled: Boolean = true,
) {

    fun toDefinition(): TemplateDefinition {
        val builder =
            TemplateDefinition.newBuilder()
                .setId(id)
                .setName(name)
                .setWeight(weight)
                .setLocation(location.toDefinition())
                .setVersion(version)
                .setEnabled(enabled)

        return builder.build()
    }

    companion object {
        fun fromDefinition(templateDefinition: TemplateDefinition): Template {
            return Template(
                templateDefinition.name,
                templateDefinition.weight,
                templateDefinition.id,
                TemplateLocation.fromDefinition(templateDefinition.location),
                templateDefinition.version,
                templateDefinition.enabled,
            )
        }

    }
}
