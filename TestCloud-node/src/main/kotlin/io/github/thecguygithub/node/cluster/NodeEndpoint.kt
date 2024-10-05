package io.github.thecguygithub.node.cluster

import io.github.thecguygithub.api.Closeable


interface NodeEndpoint : Closeable {
    fun situation(): NodeSituation?

    fun data(): NodeEndpointData?

    fun situation(situation: NodeSituation?)
}