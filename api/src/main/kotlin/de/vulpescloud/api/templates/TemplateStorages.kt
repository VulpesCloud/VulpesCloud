package de.vulpescloud.api.templates

import kotlinx.serialization.Serializable

@Serializable
enum class TemplateStorages {
    LOCAL,
    S3;
}
