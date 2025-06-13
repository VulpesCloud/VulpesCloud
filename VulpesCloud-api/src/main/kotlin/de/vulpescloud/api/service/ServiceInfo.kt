package de.vulpescloud.api.service

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.player.Player
import de.vulpescloud.api.task.Task
import java.util.*

data class ServiceInfo(
    val task: Task,
    val uuid: UUID,
    val orderedId: Int,
    val port: Int,
    val runningNode: ClusterNode,
    var state: ServiceStates,
    val maxPlayers: Int,
    val onlinePlayerCount: Int,
    val name: String = "${task.name}-$orderedId",
    val onlinePlayers: List<Player>,
    val environmentVars: List<Pair<String, String>>
)