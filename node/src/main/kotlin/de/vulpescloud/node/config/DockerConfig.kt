package de.vulpescloud.node.config

import kotlinx.serialization.Serializable

@Serializable
data class DockerConfig(
    val host: String = "unix:///var/run/docker.sock",
    val network: String = "vulpescloud",
    val dockerCertPath: String = "/certs",
    val registryUsername: String = "",
    val registryPassword: String = "",
    val registryEmail: String = ""
)
