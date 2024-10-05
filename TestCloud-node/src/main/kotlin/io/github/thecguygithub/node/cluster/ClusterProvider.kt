package io.github.thecguygithub.node.cluster

import java.io.Closeable


interface ClusterProvider : Closeable {
    fun localNode(): LocalNode?

    fun headNode(): NodeEndpoint?

    fun localHead(): Boolean

    fun find(nodeId: String?): NodeEndpoint?

    fun endpoints(): Set<NodeEndpoint?>?

    fun initialize()
}