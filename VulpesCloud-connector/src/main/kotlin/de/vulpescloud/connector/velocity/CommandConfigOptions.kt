package de.vulpescloud.connector.velocity

import de.vulpescloud.api.service.ServiceInfo
import de.vulpescloud.api.virtualconfig.VirtualConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

enum class CommandConfigOptions(val path: String, val default: String) {

    CLOUD_PREFIX("commands.cloud.prefix", "<dark_gray>[<#ffaf00>VulpesCloud</color>]</dark_gray> "),
    CLOUD_SERVICE_NOTFOUND(
        "commands.cloud.service.notfound",
        "<gray>The Service could <red>not</red> be found!</gray>",
    ),
    CLOUD_SERVICE_LIST_HEADER(
        "commands.cloud.service.list.header",
        "<gray>Available Services:</gray>",
    ),
    CLOUD_SERVICE_LIST_SERVICE(
        "commands.cloud.service.list.services",
        "<gray> - <white>%service%</white> State: <yellow>%state%</yellow> Running Node: <yellow>%runningNode%</yellow></gray>",
    ),
    CLOUD_SERVICE_CONNECT_SUCCESS(
        "commands.cloud.service.connect.success",
        "<gray>Connecting to <white>%service%</white>...</gray>",
    ),
    CLOUD_SERVICE_CONNECT_ERROR(
        "commands.cloud.service.connect.error",
        "<gray>Could not connect to <white>%service%</white>! Check the Proxy Console for more information!</gray>",
    ),
    CLOUD_SERVICE_STOP_SUCCESS(
        "commands.cloud.service.stop.success",
        "<gray>Sending Notification to <red>stop</red> Service <white>%service%</white>!</gray>",
    ),
    CLOUD_SERVICE_START_SUCCESS(
        "commands.cloud.service.start.success",
        "<gray>Sending Notification to <green>start</green> Service <white>%service%</white>!</gray>",
    );

    fun getService(config: VirtualConfig, serviceInfo: ServiceInfo): Component {
        val miniMessage = MiniMessage.miniMessage()
        val prefix = config.getEntry(CLOUD_PREFIX.path, CLOUD_PREFIX.default)
        return miniMessage.deserialize(
            (prefix + config.getEntry(path, default)).replaceCommonServicePlaceholders(serviceInfo)
        )
    }

    fun get(config: VirtualConfig): Component {
        val miniMessage = MiniMessage.miniMessage()
        val prefix = config.getEntry(CLOUD_PREFIX.path, CLOUD_PREFIX.default)
        return miniMessage.deserialize(prefix + config.getEntry(path, default))
    }
}

fun String.replaceCommonServicePlaceholders(serviceInfo: ServiceInfo): String {
    return this.replace("%service%", serviceInfo.name)
        .replace("%state%", serviceInfo.state.name)
        .replace("%runningNode%", serviceInfo.runningNode.name)
        .replace("%taskName%", serviceInfo.task.name)
}
