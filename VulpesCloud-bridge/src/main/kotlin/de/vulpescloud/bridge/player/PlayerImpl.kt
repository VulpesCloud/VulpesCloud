package de.vulpescloud.bridge.player

import de.vulpescloud.api.players.Player
import de.vulpescloud.api.services.ClusterService
import java.util.*
import java.util.concurrent.CompletableFuture

class PlayerImpl(
    val name: String,
    val uuid: UUID
) : Player {

    var currentServer: ClusterService? = null
    var currentProxy: ClusterService? = null

    override fun uniqueId(): UUID {
        return this.uuid
    }

    fun setProxy(service: ClusterService) {
        this.currentProxy = service
    }
    fun setServer(service: ClusterService) {
        this.currentServer = service
    }

    override fun currentProxyName(): String {
        val player = PlayerProvider().getPlayerByUUID(uniqueId())
        return if (PlayerProvider().isPlayerOnline(uniqueId())) {
            if (player == null) {
                throw IllegalStateException("The Player is null but is also online! ")
            } else {
                if (currentProxy == null) {
                    throw IllegalStateException("CurrentProxy is null but the Player is online!")
                } else {
                    currentProxy!!.name()
                }
            }
        } else {
            "<none>"
        }
    }

    override fun currentServerName(): String {
        val player = PlayerProvider().getPlayerByUUID(uniqueId())
        return if (PlayerProvider().isPlayerOnline(uniqueId())) {
            if (player == null) {
                throw IllegalStateException("The Player is null but is also online!")
            } else {
                if (currentServer == null) {
                    throw IllegalStateException("CurrentServer is null but the Player is online!")
                } else {
                    currentServer!!.name()
                }
            }
        } else {
            "<none>"
        }
    }

    override fun currentProxyAsync(): CompletableFuture<ClusterService?> {
        return if (currentProxy == null) {
            CompletableFuture.completedFuture(null)
        } else {
            CompletableFuture.completedFuture(currentProxy)
        }
    }

    override fun currentServerAsync(): CompletableFuture<ClusterService?> {
        return if (currentServer == null) {
            CompletableFuture.completedFuture(null)
        } else {
            CompletableFuture.completedFuture(currentServer)
        }
    }

    override fun sendTitle(title: String, subtitle: String?, fadeIn: Int, stay: Int, fadeOut: Int) {
        TODO("Not yet implemented")
    }

    override fun sendTablist(header: String, footer: String) {
        TODO("Not yet implemented")
    }

    override fun sendMessage(message: String) {
        TODO("Not yet implemented")
    }

    override fun sendActionBar(message: String) {
        TODO("Not yet implemented")
    }

    override fun connect(serviceId: String) {
        TODO("Not yet implemented")
    }

    override fun name(): String {
        return this.name
    }
}