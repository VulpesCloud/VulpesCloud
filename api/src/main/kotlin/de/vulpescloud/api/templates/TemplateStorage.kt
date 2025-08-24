package de.vulpescloud.api.templates

import kotlinx.serialization.Serializable

@Serializable
enum class TemplateStorage {
    LOCAL,
    S3;
}
