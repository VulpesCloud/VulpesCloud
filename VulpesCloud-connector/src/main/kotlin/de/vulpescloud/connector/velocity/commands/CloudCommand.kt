package de.vulpescloud.connector.velocity.commands

import com.velocitypowered.api.proxy.ProxyServer
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.api.service.ServiceActions
import de.vulpescloud.api.virtualconfig.VirtualConfig
import de.vulpescloud.bridge.VulpesBridge.getServiceProvider
import de.vulpescloud.connector.velocity.CommandConfigOptions
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.stringArgument
import kotlin.jvm.optionals.getOrNull
import net.kyori.adventure.text.minimessage.MiniMessage
import org.json.JSONObject
import org.slf4j.LoggerFactory

class CloudCommand(config: VirtualConfig, proxyServer: ProxyServer) {

    private val miniMessage = MiniMessage.miniMessage()
    private val logger = LoggerFactory.getLogger("CloudCommand")

    val command =
        commandTree("cloud") {
            literalArgument("service") {
                withAliases("services")
                literalArgument("list") {
                    executesPlayer(
                        PlayerCommandExecutor { player, _ ->
                            player.sendMessage(
                                CommandConfigOptions.CLOUD_SERVICE_LIST_HEADER.get(config)
                            )
                            getServiceProvider().services().forEach {
                                player.sendMessage(
                                    CommandConfigOptions.CLOUD_SERVICE_LIST_SERVICE.getService(
                                        config,
                                        it,
                                    )
                                )
                            }
                        }
                    )
                }

                stringArgument("service") {
                    replaceSuggestions(
                        ArgumentSuggestions.stringCollection {
                            getServiceProvider().services().map { it.name }
                        }
                    )

                    literalArgument("stop") {
                        executesPlayer(
                            PlayerCommandExecutor { player, commandArguments ->
                                val serviceName = commandArguments[0] as String
                                val service = getServiceProvider().getServiceByName(serviceName)

                                if (service == null) {
                                    player.sendMessage(
                                        CommandConfigOptions.CLOUD_SERVICE_NOTFOUND.get(config)
                                    )
                                    return@PlayerCommandExecutor
                                }

                                getRC()
                                    ?.sendMessage(
                                        JSONObject()
                                            .put("receiver", service.runningNode.name)
                                            .put(
                                                "sender",
                                                "${getServiceProvider().getLocalService().name}@${service.runningNode}",
                                            )
                                            .put("content", "SERVICE")
                                            .put("action", ServiceActions.STOP)
                                            .put("service", service.name)
                                            .toString(),
                                        RedisChannels.VULPESCLOUD_NODE_COMMUNICATION.name,
                                    )

                                player.sendMessage(
                                    CommandConfigOptions.CLOUD_SERVICE_STOP_SUCCESS.getService(
                                        config,
                                        service,
                                    )
                                )
                            }
                        )
                    }

                    literalArgument("connect") {
                        executesPlayer(
                            PlayerCommandExecutor { player, commandArguments ->
                                val serviceName = commandArguments[0] as String
                                val service = getServiceProvider().getServiceByName(serviceName)

                                if (service == null) {
                                    player.sendMessage(
                                        CommandConfigOptions.CLOUD_SERVICE_NOTFOUND.get(config)
                                    )
                                    return@PlayerCommandExecutor
                                }

                                val server = proxyServer.getServer(serviceName).getOrNull()

                                if (server == null) {
                                    player.sendMessage(
                                        CommandConfigOptions.CLOUD_SERVICE_CONNECT_ERROR.getService(
                                            config,
                                            service,
                                        )
                                    )

                                    logger.warn(
                                        "Failed to connect player ${player.username} to service $serviceName: Server not found."
                                    )

                                    return@PlayerCommandExecutor
                                } else {
                                    player.sendMessage(
                                        CommandConfigOptions.CLOUD_SERVICE_CONNECT_SUCCESS
                                            .getService(config, service)
                                    )
                                    player.createConnectionRequest(server).connectWithIndication()
                                }
                            }
                        )
                    }

                    literalArgument("start") {
                        executesPlayer(
                            PlayerCommandExecutor { player, commandArguments ->
                                val serviceName = commandArguments[0] as String
                                val service = getServiceProvider().getServiceByName(serviceName)

                                if (service == null) {
                                    player.sendMessage(
                                        CommandConfigOptions.CLOUD_SERVICE_NOTFOUND.get(config)
                                    )
                                    return@PlayerCommandExecutor
                                }

                                getRC()
                                    ?.sendMessage(
                                        JSONObject()
                                            .put("receiver", service.runningNode.name)
                                            .put(
                                                "sender",
                                                "${getServiceProvider().getLocalService().name}@${service.runningNode}",
                                            )
                                            .put("content", "SERVICE")
                                            .put("action", ServiceActions.START)
                                            .put("service", service.name)
                                            .toString(),
                                        RedisChannels.VULPESCLOUD_NODE_COMMUNICATION.name,
                                    )

                                player.sendMessage(
                                    CommandConfigOptions.CLOUD_SERVICE_START_SUCCESS.getService(
                                        config,
                                        service,
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
}
