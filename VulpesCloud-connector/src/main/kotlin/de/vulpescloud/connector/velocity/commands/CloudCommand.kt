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

    val miniMessage = MiniMessage.miniMessage()

    fun registerCloudCommand() {
        commandManager.buildAndRegister("cloud") {
            literal("service")
            literal("list")
            handler { context ->
                val source = context.sender()
                source.sendMessage(miniMessage.deserialize("The following &b${ServiceProvider.services().size} &7services are registered&8:"))
                ServiceProvider.services().forEach { service ->
                    source.sendMessage(miniMessage.deserialize("&8- &f${service.name()} &8: (&7${service.details()}&8)"))
                }
            }
        }
        commandManager.buildAndRegister("cloud") {
            literal("service")
            literal("stopAll")
            handler { context ->
                val source = context.sender()
                source.sendMessage(miniMessage.deserialize("Stopping all Services!"))
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
                    source.sendMessage(miniMessage.deserialize("Service not found!"))
                    return@handler
                }

                source.sendMessage(miniMessage.deserialize("Trying to stop the Service!"))
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
