package de.vulpescloud.connector.velocity.commands

import com.velocitypowered.api.command.CommandSource
import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.api.redis.builders.services.ServiceActionMessageBuilder
import de.vulpescloud.api.services.ServiceActions
import de.vulpescloud.bridge.service.ServiceProvider
import de.vulpescloud.wrapper.Wrapper
import net.kyori.adventure.text.minimessage.MiniMessage
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.velocity.VelocityCommandManager

class CloudCommand(
    private val commandManager: VelocityCommandManager<CommandSource>,
) {

    private val miniMessage = MiniMessage.miniMessage()

    fun registerCloudCommand() {
        commandManager.buildAndRegister("cloud") {
            literal("service")
            literal("list")
            handler { context ->
                val source = context.sender()
                source.sendMessage(miniMessage.deserialize("<gray>The following <aqua>${ServiceProvider.services().size}</aqua> services are registered<black>:</black></gray>"))
                ServiceProvider.services().forEach { service ->
                    source.sendMessage(miniMessage.deserialize("<gray><black>-</black> <white>${service.name()}</white> <black>:</black> <black>(${service.details()})</black> services are registered</gray>"))
                }
            }
        }
        commandManager.buildAndRegister("cloud") {
            literal("service")
            literal("stopAll")
            handler { context ->
                val source = context.sender()
                source.sendMessage(miniMessage.deserialize("<gray>Stopping all Services!</gray>"))
                ServiceProvider.services().forEach {
                    Wrapper.instance.getRC()?.sendMessage(
                        ServiceActionMessageBuilder
                            .setService(it)
                            .setAction(ServiceActions.STOP)
                            .build(),
                        RedisChannelNames.VULPESCLOUD_SERVICE_ACTION.name
                    )
                }
            }
        }
        commandManager.buildAndRegister("cloud") {
            literal("service")
            required("service", StringParser.stringParser())
            literal("stop")

            handler { context ->
                val source = context.sender()
                val service = ServiceProvider.findServiceByName(context["service"])
                if (service == null) {
                    source.sendMessage(miniMessage.deserialize("<gray>Service not found!</gray>"))
                    return@handler
                }

                source.sendMessage(miniMessage.deserialize("<gray>Trying to stop the Service!</gray>"))
                Wrapper.instance.getRC()?.sendMessage(
                    ServiceActionMessageBuilder
                        .setService(service)
                        .setAction(ServiceActions.STOP)
                        .build(),
                    RedisChannelNames.VULPESCLOUD_SERVICE_ACTION.name
                )
            }
        }

    }
}
