package de.vulpescloud.node.config

import de.vulpescloud.api.Detail


@JvmRecord
data class RedisEndpointData(val user: String, val hostname: String, val port: Int, val password: String) : Detail {
    override fun details(): String {
        return "hostname&8=&7" + this.hostname + "&8, &7port&8=&7" + this.port
    }
}