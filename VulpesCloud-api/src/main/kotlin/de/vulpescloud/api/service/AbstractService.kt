package de.vulpescloud.api.service

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.player.Player
import de.vulpescloud.api.task.Task
import java.util.*

abstract class AbstractService {

    abstract val task: Task
    abstract val uuid: UUID
    abstract val orderedId: Int
    abstract val port: Int
    abstract val runningNode: ClusterNode
    abstract var state: ServiceStates
    abstract val maxPlayers: Int
    abstract val onlinePlayerCount: Int
    abstract val name: String
    abstract val onlinePlayers: List<Player>
    abstract val environmentVars: List<Pair<String, String>>

    abstract fun start()

    abstract fun stop()

    abstract fun forceStop()

    abstract fun sendCommand(command: String)

    fun getServiceInfo(): ServiceInfo = ServiceInfo(
        task,
        uuid,
        orderedId,
        port,
        runningNode,
        state,
        maxPlayers,
        onlinePlayerCount,
        name,
        onlinePlayers,
        environmentVars
    )

}