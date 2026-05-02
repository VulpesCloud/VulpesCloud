package de.vulpescloud.connector.velocity

import build.buf.gen.vulpescloud.events.v1.playerJoinEvent
import build.buf.gen.vulpescloud.events.v1.playerQuitEvent
import build.buf.gen.vulpescloud.events.v1.playerSwitchServerEvent
import build.buf.gen.vulpescloud.events.v1.serviceStateChangedEvent
import build.buf.gen.vulpescloud.players.v1.offlinePlayer
import build.buf.gen.vulpescloud.players.v1.onlinePlayer
import build.buf.gen.vulpescloud.services.v1.UpdatePlayerCountRequest
import build.buf.gen.vulpescloud.virtualconfig.v1.createVirtualConfigRequest
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.bridge.BridgeAPI
import de.vulpescloud.connector.velocity.commands.CloudCommand
import de.vulpescloud.connector.velocity.commands.HubCommand
import de.vulpescloud.connector.velocity.config.ConnectorConfig
import de.vulpescloud.connector.velocity.events.PlayerChooseInitialServerEventListener
import de.vulpescloud.wrapper.Wrapper
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIVelocityConfig
import jakarta.inject.Inject
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.bstats.velocity.Metrics
import org.slf4j.Logger

@Plugin(id = "vulpescloud-connector", name = "VulpesCloud-Connector", authors = ["TheCGuy"])
@Suppress("unused")
class VelocityConnector
@Inject
constructor(
    private val eventManager: EventManager,
    private val proxyServer: ProxyServer,
    private val metricsFactory: Metrics.Factory,
    private val logger: Logger,
) {
    private lateinit var metrics: Metrics
    private val pluginID = 27325
    private val bridgeAPI = BridgeAPI.createFutureAPI()
    private lateinit var velocityServerRegistrationHandler: VelocityServerRegistrationHandler

    @Subscribe
    fun onProxyInitializeEvent(event: ProxyInitializeEvent) {
        metrics = metricsFactory.make(this, pluginID)

        CommandAPI.onLoad(CommandAPIVelocityConfig(proxyServer, this))

        velocityServerRegistrationHandler =
            VelocityServerRegistrationHandler(proxyServer, bridgeAPI)

        eventManager.register(this, PlayerChooseInitialServerEventListener(bridgeAPI, proxyServer))

        runBlocking {
            bridgeAPI
                .getCoroutineVirtualConfigAPI()
                .stub
                .createVirtualConfig(
                    createVirtualConfigRequest {
                        this.name = "vc_connector"
                        this.config = Json.encodeToString(ConnectorConfig())
                    }
                )
        }

        val localService =
            try {
                bridgeAPI.getServicesAPI().getLocalService().get(5, TimeUnit.SECONDS)
            } catch (ex: Exception) {
                logger.error("Exception while trying to get local service!")
                logger.error(
                    "Grpc Connection state: ${Wrapper.instance.grpcClient.channel.getState(true)}"
                )
                ex.printStackTrace()
                null
            }

        if (localService == null) {
            logger.error("LocalService is null!")
            logger.error(
                "Grpc Connection state: ${Wrapper.instance.grpcClient.channel.getState(true)}"
            )
            return
        }

        HubCommand(proxyServer, bridgeAPI).command.register()
        CloudCommand()

        bridgeAPI
            .getEventAPI()
            .publish(
                serviceStateChangedEvent {
                    this.service = localService.toDefinition()
                    this.oldState = localService.state.toServiceState()
                    this.newState = ServiceStates.RUNNING.toServiceState()
                },
                true,
            )
    }

    @Subscribe
    fun onProxyShutdownEvent(event: ProxyShutdownEvent) {
        metrics.shutdown()
        bridgeAPI.getEventAPI().shutdown()
    }

    @Subscribe
    fun onLoginEvent(event: LoginEvent) {
        runBlocking {
            val service = bridgeAPI.getServicesAPI().getLocalService().get()!!
            Wrapper.instance.grpcClient.serviceAPI.updatePlayerCount(
                UpdatePlayerCountRequest.newBuilder()
                    .setPlayerCount(proxyServer.playerCount)
                    .setService(service.toDefinition())
                    .build()
            )
            bridgeAPI
                .getEventAPI()
                .publish(
                    playerJoinEvent {
                        this.player = onlinePlayer {
                            this.name = event.player.username
                            this.uuid = event.player.uniqueId.toString()
                            this.proxyServiceName = service.name()
                            this.serverServiceName = ""
                        }
                        this.timestamp = System.currentTimeMillis()
                    },
                    true,
                )
        }
    }

    @Subscribe
    fun onDisconnectEvent(event: DisconnectEvent) {
        runBlocking {
            val service = bridgeAPI.getServicesAPI().getLocalService().get()!!
            Wrapper.instance.grpcClient.serviceAPI.updatePlayerCount(
                UpdatePlayerCountRequest.newBuilder()
                    .setPlayerCount(proxyServer.playerCount)
                    .setService(service.toDefinition())
                    .build()
            )
            val player =
                bridgeAPI
                    .getPlayerAPI()
                    .getRegisteredPlayerByUUID(event.player.uniqueId.toString())
                    .get()!!

            bridgeAPI
                .getEventAPI()
                .publish(
                    playerQuitEvent {
                        this.player = offlinePlayer {
                            this.name = event.player.username
                            this.uuid = event.player.uniqueId.toString()
                            this.firstSeen = player.firstSeen
                            this.lastSeen = System.currentTimeMillis()
                        }
                        this.lastProxyName =
                            bridgeAPI.getServicesAPI().getLocalService().get()!!.name()
                        this.lastServerName = ""
                        this.timestamp = System.currentTimeMillis()
                    },
                    true,
                )
        }
    }

    @Subscribe
    fun onServerConnectedEvent(event: ServerConnectedEvent) {
        CoroutineScope(Dispatchers.IO).launch {
            val player =
                bridgeAPI
                    .getPlayerAPI()
                    .getOnlinePlayerByUUID(event.player.uniqueId.toString())
                    .get()

            if (player == null) {
                logger.error("Unable to find player for UUID ${event.player.uniqueId}!")
                logger.error("Player is null!")
                logger.info(
                    "DBG: ${bridgeAPI.getPlayerAPI().getAllOnlinePlayers().get().joinToString { it.name + " (${it.uuid})" }}"
                )
                return@launch
            }

            bridgeAPI
                .getEventAPI()
                .publish(
                    playerSwitchServerEvent {
                        this.player = onlinePlayer {
                            this.name = player.name
                            this.uuid = player.uuid
                            this.proxyServiceName =
                                bridgeAPI.getServicesAPI().getLocalService().get()!!.name()
                            this.serverServiceName = event.server.serverInfo.name
                        }
                        this.oldServer = event.previousServer.getOrNull()?.serverInfo?.name ?: ""
                        this.newServer = event.server.serverInfo.name
                        this.timestamp = System.currentTimeMillis()
                    },
                    true,
                )
        }
    }
}
