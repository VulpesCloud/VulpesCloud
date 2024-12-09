package de.vulpescloud.bridge.service

import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.wrapper.Wrapper
import org.json.JSONObject
import java.util.*
import java.util.concurrent.CompletableFuture

open class Service(
    private val task: Task,
    private val orderedId: Int,
    private val id: UUID,
    private val port: Int,
    private val hostname: String,
    private val runningNode: String,
    private var maxPlayers: Int = task.maxPlayers(),
    private var state: ServiceStates = ServiceStates.LOADING,
    private var logging: Boolean = false
) : Service {

    fun details(): String {
        return "id&8=&7$id&8, &7hostname&8=&7$hostname, &7port&8=&7$port&8, &7node&8=&7$runningNode&8, &7state&8=&7${state.name}"
    }

    fun setLogging(logging: Boolean) {
        this.logging = logging
    }

    fun setState(state: ServiceStates) {
        this.state = state
        // todo FIX THIS: This always produces a empty JSON
        // Wrapper.instance.getRC()?.setHashField(RedisHashNames.VULPESCLOUD_SERVICES.name, name(), JSONObject(ServiceProvider.findServiceByName(Wrapper.instance.service.name)).toString())
    }

    fun logging(): Boolean {
        return this.logging
    }
    override fun task(): Task {
        return task
    }

    override fun orderedId(): Int {
        return orderedId
    }

    override fun id(): UUID {
        return id
    }

    override fun hostname(): String {
        return hostname
    }

    override fun port(): Int {
        return port
    }

    override fun runningNode(): String {
        return runningNode
    }


    override fun state(): ServiceStates {
        return state
    }

    override fun maxPlayers(): Int {
        return maxPlayers
    }

    override fun onlinePlayersCountAsync(): CompletableFuture<Int> {
        throw IllegalStateException("Sklahsbglidzgjhnklvcxybc,ui")
        //return CompletableFuture.completedFuture(onlinePlayers()?.size)
    }
//
//    override fun onlinePlayersAsync(): CompletableFuture<List<Player?>?> {
//        return CompletableFuture.completedFuture(
//            null
////            JavaCloudAPI.getInstance().playerProvider().players()
////                ?.filter { it?.currentServer()?.id() == id || it?.currentProxy()?.id() == id }
//        )
//    }
}