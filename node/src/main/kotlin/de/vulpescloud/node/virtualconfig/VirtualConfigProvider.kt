package de.vulpescloud.node.virtualconfig

import build.buf.gen.vulpescloud.virtualconfig.v1.configOrNull
import build.buf.gen.vulpescloud.virtualconfig.v1.getByNameRequest
import build.buf.gen.vulpescloud.virtualconfig.v1.updateVirtualConfigRequest
import de.vulpescloud.api.virtualconfig.VirtualConfig
import de.vulpescloud.node.Node
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.nio.file.Path
import kotlin.io.path.Path

class VirtualConfigProvider {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        isLenient = true
    }

    val tempConfigsPath: Path = Path("temp").resolve("configs")

    suspend inline fun <reified T> getCustomConfigObject(
        name: String,
        forceGet: Boolean = false,
    ): T? {
        val file = tempConfigsPath.resolve("$name.json").toFile()
        if (!forceGet) {
            if (file.exists()) {
                getLocalConfigJson(name)?.let {
                    return json.decodeFromString<T>(it)
                }
            }
        }
        val config =
            Node.instance.localGrpcClient.virtualConfigAPI
                .getByName(getByNameRequest { this.name = name })
                .configOrNull

        if (config == null) {
            return null
        }

        file.parentFile.mkdirs()
        file.writeText(JSONObject(VirtualConfig.fromDefinition(config)).toString())
        return json.decodeFromString<T>(config.config ?: return null)
    }

    suspend inline fun <reified T> getCustomConfigObject(
        config: VirtualConfig,
        forceGet: Boolean = false,
    ): T? = getCustomConfigObject<T>(config.name, forceGet)

    suspend fun getCustomConfig(name: String, forceGet: Boolean = false): VirtualConfig? {
        val file = tempConfigsPath.resolve("$name.json").toFile()
        if (!forceGet) {

            return getLocalConfig(name)
        }
        val config =
            Node.instance.localGrpcClient.virtualConfigAPI
                .getByName(getByNameRequest { this.name = name })
                .configOrNull
        if (config == null) {
            return null
        }

        file.parentFile.mkdirs()
        file.writeText(config.config)
        return VirtualConfig(
            config.name,
            config.createdAt,
            config.lastUpdatedAt,
            JSONObject(config.config),
        )
    }

    suspend inline fun <reified T> updateCustomConfig(config: VirtualConfig, value: T) {
        Node.instance.localGrpcClient.virtualConfigAPI.updateVirtualConfig(
            updateVirtualConfigRequest { this.config = json.encodeToString(value) }
        )
        val file = tempConfigsPath.resolve("${config.name}.json").toFile()
        file.parentFile.mkdirs()
        file.writeText(config.config.toString(4))
    }

    suspend inline fun <reified T> updateCustomConfig(name: String, value: T) {
        Node.instance.localGrpcClient.virtualConfigAPI.updateVirtualConfig(
            updateVirtualConfigRequest { this.config = json.encodeToString(value) }
        )
        val file = tempConfigsPath.resolve("$name.json").toFile()
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(value))
    }

    suspend fun updateCustomConfig(config: VirtualConfig) {
        Node.instance.localGrpcClient.virtualConfigAPI.updateVirtualConfig(
            updateVirtualConfigRequest { this.config = config.config.toString() }
        )
        val file = tempConfigsPath.resolve("${config.name}.json").toFile()
        file.parentFile.mkdirs()
        file.writeText(config.config.toString(4))
    }

    fun getLocalConfigJson(name: String): String? {
        return getLocalConfig(name)?.config?.toString(4)
    }

    fun getLocalConfig(name: String): VirtualConfig? {
        val config = tempConfigsPath.resolve("$name.json").toFile()
        if (!config.exists()) {
            return null
        }
        val json = JSONObject(config.readText())
        return VirtualConfig(
            json.getString("name"),
            json.getLong("createdAt"),
            json.getLong("lastUpdatedAt"),
            json.getJSONObject("config"),
        )
    }

    suspend fun updateLocalConfigFromDatabase(name: String) {
        val newConfig = getCustomConfig(name, true) ?: return
        val file = tempConfigsPath.resolve("${name}.json").toFile()
        file.parentFile.mkdirs()
        file.writeText(newConfig.config.toString(4))
    }

    suspend fun updateLocalConfigFromDatabase(config: VirtualConfig) {
        val newConfig = getCustomConfig(config.name, true) ?: return
        val file = tempConfigsPath.resolve("${config.name}.json").toFile()
        file.parentFile.mkdirs()
        file.writeText(newConfig.config.toString(4))
    }

    suspend fun updateDatabaseFromLocalConfig(name: String) {
        val json = getLocalConfigJson(name) ?: return
        Node.instance.localGrpcClient.virtualConfigAPI.updateVirtualConfig(
            updateVirtualConfigRequest { this.config = json }
        )
    }

    suspend fun updateDatabaseFromLocalConfig(config: VirtualConfig) {
        val json = getLocalConfigJson(config.name) ?: return
        Node.instance.localGrpcClient.virtualConfigAPI.updateVirtualConfig(
            updateVirtualConfigRequest { this.config = json }
        )
    }

    fun deleteLocalConfig(name: String) {
        val file = tempConfigsPath.resolve("$name.json").toFile()
        if (file.exists()) {
            file.delete()
        }
    }
}
