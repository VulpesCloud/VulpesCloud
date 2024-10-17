package io.github.thecguygithub.node.tasks

import io.github.thecguygithub.api.platforms.PlatformGroupDisplay
import io.github.thecguygithub.api.tasks.AbstractClusterTask


open class ClusterTaskImpl(
    name: String?,
    platform: PlatformGroupDisplay?,
    templates: Array<String?>?,
    nodes: Array<String?>?,
    maxMemory: Int,
    maxPlayers: Int,
    staticService: Boolean,
    minOnlineCount: Int,
    maintenance: Boolean,
    startPort: Int

) :
    AbstractClusterTask(
        name,
        platform,
        templates,
        nodes,
        maxMemory,
        maxPlayers,
        staticService,
        minOnlineCount,
        maintenance,
        startPort
    ) {
    override fun update() {
        //todo
    }
}