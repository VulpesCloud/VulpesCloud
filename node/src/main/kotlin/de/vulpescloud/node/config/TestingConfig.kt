package de.vulpescloud.node.config

import kotlinx.serialization.Serializable

@Serializable
data class TestingConfig(
    val newServiceLoggingStyle: Boolean = false,
)
