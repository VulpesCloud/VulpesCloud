package de.vulpescloud.connector.velocity

import de.vulpescloud.api.service.Service
import de.vulpescloud.api.virtualconfig.VirtualConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

enum class CommandConfigOptions(val path: String, val default: String) {

    CLOUD_SERVICE_NOTFOUND("commands.cloud.service.notfound", "<gray>The Service could <red>not</red> be found!</gray>"),
    CLOUD_SERVICE_LIST_HEADER("commands.cloud.service.list.header", "<gray>Available Services:</gray>"),
    CLOUD_SERVICE_LIST_SERVICE("commands.cloud.service.list.services", "<gray> - <white>%service%</white> State: <yellow>%state%</yellow> Running Node: <yellow>%runningNode%</yellow></gray>"),
    CLOUD_SERVICE_CONNECT_SUCCESS("commands.cloud.service.connect.success","<gray>Connecting to <white>%service%</white>...</gray>"),
    CLOUD_SERVICE_CONNECT_ERROR("commands.cloud.service.connect.error","<gray>Could not connect to <white>%service%</white>! Check the Proxy Console for more information!</gray>"),
    CLOUD_SERVICE_STOP_SUCCESS("commands.cloud.service.stop.success", "<gray>Stopping Service <white>%service%</white>!</gray>");

    fun getService(config: VirtualConfig, service: Service): Component {
        val miniMessage = MiniMessage.miniMessage()
        return miniMessage.deserialize(config.getEntry(path, default).replaceCommonServicePlaceholders(service))
    }

    fun get(config: VirtualConfig): Component {
        val miniMessage = MiniMessage.miniMessage()
        return miniMessage.deserialize(config.getEntry(path, default))
    }
}

fun String.replaceCommonServicePlaceholders(
    service: Service,
): String {
    return this
        .replace("%service%", service.name)
        .replace("%state%", service.state.name)
        .replace("%runningNode%", service.runningNode.name)
        .replace("%taskName%", service.task.name)
}
