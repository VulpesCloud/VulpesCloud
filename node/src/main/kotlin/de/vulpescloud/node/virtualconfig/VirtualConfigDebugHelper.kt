package de.vulpescloud.node.virtualconfig

import build.buf.gen.vulpescloud.virtualconfig.v1.createVirtualConfigRequest
import de.vulpescloud.node.Node
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

object VirtualConfigDebugHelper {

    private val logger = LoggerFactory.getLogger("VirtualConfigDebugHelper")

    suspend fun createDebugConfig() {
        logger.info("Creating debug config...")
        Node.instance.localGrpcClient.virtualConfigAPI.createVirtualConfig(
            createVirtualConfigRequest {
                this.name = "debug_config"
                this.config = Json.encodeToString(DebugConfig(true, AnotherDebugConfig("test")))
            }
        )
        logger.info("Debug config created!")
    }

    suspend fun updateDebugConfig() {
        logger.info("Updating debug config...")
        Node.instance.virtualConfigProvider.updateCustomConfig(
            "debug_config",
            DebugConfig(false, AnotherDebugConfig("test2")),
        )
        logger.info("Debug config updated!")
    }


}

@Serializable data class AnotherDebugConfig(val testing: String)

@Serializable data class DebugConfig(val enabled: Boolean, val anotherConfig: AnotherDebugConfig)
