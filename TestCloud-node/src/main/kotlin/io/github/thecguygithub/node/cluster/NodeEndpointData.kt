package io.github.thecguygithub.node.cluster

import io.github.thecguygithub.api.Detail


@JvmRecord
data class NodeEndpointData(val name: String, val hostname: String, val port: Int) : Detail {
    override fun details(): String {
        return "hostname&8=&7" + this.hostname + "&8, &7port&8=&7" + this.port
    }
}