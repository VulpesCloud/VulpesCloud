package de.vulpescloud.node.players

import build.buf.gen.vulpescloud.events.v1.PlayerJoinEvent
import build.buf.gen.vulpescloud.events.v1.PlayerQuitEvent
import build.buf.gen.vulpescloud.events.v1.PlayerSwitchServerEvent
import de.vulpescloud.api.players.OfflinePlayer
import de.vulpescloud.api.players.toAPI
import de.vulpescloud.node.Node
import de.vulpescloud.node.event.EventsService
import de.vulpescloud.node.utils.PropertyUtils.isLoggingPlayerEvents
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.slf4j.LoggerFactory

object PlayerJoinEventListener {
    private var job: Job? = null
    private val logger = LoggerFactory.getLogger(PlayerJoinEventListener::class.java)
    private val offlinePlayerDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("offlinePlayers")
    }

    fun subscribe() {
        job =
            EventsService.subscribe<PlayerJoinEvent> { event ->
                val proxyName = event.player.proxyServiceName
                if (isLoggingPlayerEvents()) {
                    logger.info(
                        "Player <aqua>${event.player.name}</aqua> joined the Network on proxy <light_purple>$proxyName</light_purple>"
                    )
                }
                if (Node.instance.nodeServices.any { it.service.name() == proxyName }) {
                    Node.instance.nodeProxyPlayers
                        .getOrPut(proxyName, ::CopyOnWriteArrayList)
                        .add(event.player.toAPI())
                }

                if (offlinePlayerDatabase.get(event.player.uuid) == null) {
                    if (isLoggingPlayerEvents()) {
                        logger.info(
                            "Player <aqua>${event.player.name}</aqua> joined the Network for the first time!"
                        )
                    }
                    offlinePlayerDatabase.insertIgnore(
                        event.player.uuid,
                        Json.encodeToJsonElement(
                            OfflinePlayer(
                                event.player.name,
                                event.player.uuid,
                                System.currentTimeMillis(),
                                System.currentTimeMillis(),
                            )
                        ),
                    )
                }
            }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}

object PlayerQuitEventListener {
    private var job: Job? = null
    private val logger = LoggerFactory.getLogger(PlayerQuitEventListener::class.java)
    private val offlinePlayerDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("offlinePlayers")
    }

    fun subscribe() {
        job =
            EventsService.subscribe<PlayerQuitEvent> { event ->
                val proxyName = event.lastProxyName
                if (isLoggingPlayerEvents()) {
                    logger.info("Player <aqua>${event.player.name}</aqua> left the Network!")
                }
                if (Node.instance.nodeServices.any { it.service.name() == proxyName }) {
                    Node.instance.nodeProxyPlayers
                        .getOrPut(proxyName, ::CopyOnWriteArrayList)
                        .removeIf { it.uuid == event.player.uuid }
                }

                offlinePlayerDatabase.upsert(
                    event.player.uuid,
                    Json.encodeToJsonElement(event.player.toAPI()),
                )
            }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}

object PlayerSwitchServerEventListener {
    private var job: Job? = null
    private val logger = LoggerFactory.getLogger(PlayerSwitchServerEventListener::class.java)

    fun subscribe() {
        job =
            EventsService.subscribe<PlayerSwitchServerEvent> { event ->
                val oldServerName = event.oldServer
                val newServerName = event.newServer
                if (isLoggingPlayerEvents()) {
                    logger.info(
                        "Player <aqua>${event.player.name}</aqua> switched servers from <light_purple>$oldServerName</light_purple> to <light_purple>$newServerName</light_purple>!"
                    )
                }
                if (Node.instance.nodeServices.any { it.service.name() == oldServerName }) {
                    Node.instance.nodeServerPlayers
                        .getOrPut(oldServerName, ::CopyOnWriteArrayList)
                        .removeIf { it.uuid == event.player.uuid }
                }
                if (Node.instance.nodeServices.any { it.service.name() == newServerName }) {
                    Node.instance.nodeServerPlayers
                        .getOrPut(newServerName, ::CopyOnWriteArrayList)
                        .add(event.player.toAPI())
                }

                if (
                    Node.instance.nodeServices.any {
                        it.service.name() == event.player.proxyServiceName
                    }
                ) {
                    Node.instance.nodeProxyPlayers
                        .getOrPut(event.player.proxyServiceName, ::CopyOnWriteArrayList)
                        .apply {
                            removeIf { it.uuid == event.player.uuid }
                            add(event.player.toAPI())
                        }
                }
            }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}
