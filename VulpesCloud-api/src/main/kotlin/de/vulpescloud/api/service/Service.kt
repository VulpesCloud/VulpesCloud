package de.vulpescloud.api.service

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.player.Player
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.api.redis.SenderUtils
import de.vulpescloud.api.task.Task
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import org.json.JSONObject
import java.nio.file.Path
import java.util.UUID

data class Service(
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
) {
    fun path(): Path {
        return if (task.staticServices) {
            Path.of("local/services/${task.name}/$name")
        } else {
            Path.of("temp/services/${task.name}/$name")
        }
    }

    fun shutdown() {
        getRC()?.sendMessage(
            JSONObject()
                .put("action", ServiceActions.STOP.name)
                .put("serviceName", name)
                .put("receiver", runningNode.name)
                .put("sender", SenderUtils.getSenderName())
                .toString(),
            RedisChannels.VULPESCLOUD_ACTION_SERVICE.name
        )
    }
}
