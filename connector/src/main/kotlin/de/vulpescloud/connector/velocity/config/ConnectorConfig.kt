package de.vulpescloud.connector.velocity.config

import de.vulpescloud.bridge.BridgeAPI
import kotlinx.serialization.Serializable

@Serializable
data class ConnectorConfig(
    val hubCommandConfig: HubCommandConfig = HubCommandConfig(),
    val disconnectNoAvailableServerMessage: String =
        "<red>There is no available server for you to connect to!</red>",
    val prefix: String = "<gray>[<gradient:#EE660A:#D9BC40>VulpesCloud</gradient>]</gray> ",
)

@Serializable data class HubCommandConfig(val enabled: Boolean = true)

private val bridgeAPI = BridgeAPI.createCoroutineAPI()

suspend fun getConfig(): ConnectorConfig {
    return bridgeAPI
        .getVirtualConfigAPI()
        .getCustomConfigObject("vc_connector", ConnectorConfig.serializer(), false)
        ?: throw IllegalStateException("ConnectorConfig not found!")
}
