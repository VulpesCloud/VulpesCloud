package de.vulpescloud.api.cluster

import java.util.UUID

interface ClusterProvider {

    fun nodes(): List<ClusterNode>

    fun nodeByUUID(uuid: UUID): ClusterNode?

    fun nodeByName(name: String): ClusterNode?

    fun getHeadNode(): ClusterNode?

    fun filterByState(state: NodeStates): List<ClusterNode>

}
