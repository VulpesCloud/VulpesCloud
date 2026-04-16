package de.vulpescloud.connector.velocity.config

import de.vulpescloud.api.services.Service
import de.vulpescloud.bridge.BridgeAPI
import kotlinx.serialization.Serializable

@Serializable
data class ConnectorConfig(
    val hubCommandConfig: HubCommandConfig = HubCommandConfig(),
    val cloudCommandConfig: CloudCommandConfig = CloudCommandConfig(),
    val disconnectNoAvailableServerMessage: String =
        "<red>There is no available server for you to connect to!</red>",
    val prefix: String = "<gray>[<gradient:#EE660A:#D9BC40>VulpesCloud</gradient>]</gray> ",
)

@Serializable data class HubCommandConfig(val enabled: Boolean = true)

@Serializable
data class CloudCommandConfig(
    val enabled: Boolean = true,
    val serviceNotFound: String = "<gray>The Service could <red>not</red> be found!</gray>",
    val serviceListHeader: String = "<gray>Available Services:</gray>",
    val serviceListElement: String =
        "<gray> - <white>%service%</white> State: <yellow>%state%</yellow> Running Node: <yellow>%runningNode%</yellow></gray>",
    val serviceConnectPending: String = "<gray>Connecting to <white>%service%</white>...</gray>",
    val serviceConnectError: String =
        "<gray>Could not connect to <white>%service%</white>! Check the Proxy Console for more information!</gray>",
    val cloudServiceStopSuccess: String =
        "<gray>Sending Notification to <red>stop</red> Service <white>%service%</white>!</gray>",
    val cloudServiceStartSuccess: String =
        "<gray>Sending Notification to <green>start</green> Service <white>%service%</white>!</gray>",
)

fun String.replaceCommonServicePlaceholders(service: Service): String {
    return this.replace("%service%", "${service.task.name}-${service.orderedId}")
        .replace("%state%", service.state.name)
        .replace("%runningNode%", service.node)
        .replace("%taskName%", service.task.name)
}

private val bridgeAPI = BridgeAPI.createCoroutineAPI()

suspend fun getConfig(): ConnectorConfig {
    return bridgeAPI
        .getVirtualConfigAPI()
        .getCustomConfigObject("vc_connector", ConnectorConfig.serializer(), false)
        ?: throw IllegalStateException("ConnectorConfig not found!")
}
