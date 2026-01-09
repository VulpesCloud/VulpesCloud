package de.vulpescloud.api.serversoftware

import kotlinx.serialization.Serializable

@Serializable
enum class SoftwareType {

    SERVER,
    PROXY;

}
