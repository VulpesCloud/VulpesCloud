package de.vulpescloud.connector.velocity.commands

import build.buf.gen.vulpescloud.services.v1.stopServiceRequest
import de.vulpescloud.bridge.BridgeAPI
import de.vulpescloud.connector.velocity.config.getConfig
import de.vulpescloud.connector.velocity.config.replaceCommonServicePlaceholders
import de.vulpescloud.wrapper.Wrapper
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.minimessage.MiniMessage

class CloudCommand {

    private val miniMessage = MiniMessage.miniMessage()
    private val bridgeAPI = BridgeAPI.createCoroutineAPI()

    val command =
        commandTree("cloud") {
            withPermission("vulpescloud.commands.cloud")
            literalArgument("services") {
                literalArgument("list") {
                    withPermission("vulpescloud.commands.cloud.services.list")
                    playerExecutor { sender, _ ->
                        runBlocking {
                            val services = bridgeAPI.getServicesAPI().getAllServices()

                            sender.sendMessage(
                                miniMessage.deserialize(
                                    getConfig().prefix +
                                        getConfig().cloudCommandConfig.serviceListHeader
                                )
                            )
                            services.forEach {
                                sender.sendMessage(
                                    miniMessage.deserialize(
                                        getConfig().prefix +
                                            getConfig()
                                                .cloudCommandConfig
                                                .serviceListElement
                                                .replaceCommonServicePlaceholders(it)
                                    )
                                )
                            }
                        }
                    }
                }
                literalArgument("service") {
                    stringArgument("serviceName") {
                        replaceSuggestions(
                            ArgumentSuggestions.stringCollectionAsync {
                                CompletableFuture.supplyAsync {
                                    runBlocking {
                                        bridgeAPI.getServicesAPI().getAllServices().map {
                                            "${it.task.name}-${it.orderedId}"
                                        }
                                    }
                                }
                            }
                        )

                        literalArgument("stop") {
                            playerExecutor { sender, args ->
                                runBlocking {
                                    val serviceName =
                                        (args["serviceName"]
                                            ?: throw IllegalArgumentException(
                                                "Service name cannot be null!"
                                            ))
                                            as String
                                    val service =
                                        bridgeAPI.getServicesAPI().getServiceByName(serviceName)

                                    if (service == null) {
                                        sender.sendMessage(
                                            miniMessage.deserialize(
                                                getConfig().prefix +
                                                    getConfig().cloudCommandConfig.serviceNotFound
                                            )
                                        )
                                        return@runBlocking
                                    }

                                    Wrapper.instance.grpcClient.serviceAPI.stopService(
                                        stopServiceRequest { this.service = service.toDefinition() }
                                    )
                                    sender.sendMessage(
                                        miniMessage.deserialize(
                                            getConfig().prefix +
                                                getConfig()
                                                    .cloudCommandConfig
                                                    .cloudServiceStopSuccess
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
}
