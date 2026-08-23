package de.vulpescloud.node.virtualconfig

import build.buf.gen.vulpescloud.virtualconfig.v1.UpdateVirtualConfigRequest
import build.buf.gen.vulpescloud.virtualconfig.v1.configOrNull
import build.buf.gen.vulpescloud.virtualconfig.v1.getByNameRequest
import build.buf.gen.vulpescloud.virtualconfig.v1.updateVirtualConfigRequest
import de.vulpescloud.api.virtualconfig.VirtualConfig
import de.vulpescloud.node.Node
import java.nio.file.Path
import kotlin.io.path.Path
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.slf4j.LoggerFactory

@Suppress("Unused")
class VirtualConfigProvider {

    val logger = LoggerFactory.getLogger(VirtualConfigProvider::class.java)!!

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        isLenient = true
    }

    val tempConfigsPath: Path = Path("temp").resolve("configs")

    fun VirtualConfig.internalToJsonObject(): JSONObject =
        JSONObject()
            .put("name", name)
            .put("createdAt", createdAt)
            .put("lastUpdatedAt", lastUpdatedAt)
            .put("config", config)

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
        file.writeText(VirtualConfig.fromDefinition(config).internalToJsonObject().toString())
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
        file.writeText(VirtualConfig.fromDefinition(config).internalToJsonObject().toString())
        return VirtualConfig(
            config.name,
            config.createdAt,
            config.lastUpdatedAt,
            JSONObject(config.config),
        )
    }

    suspend inline fun <reified T> updateCustomConfig(config: VirtualConfig, value: T) {
        val response =
            Node.instance.virtualConfigServiceImpl.updateVirtualConfig(
                updateVirtualConfigRequest {
                    this.config = json.encodeToString(value)
                    this.name = config.name
                }
            )
        val file = tempConfigsPath.resolve("${config.name}.json").toFile()
        file.parentFile.mkdirs()
        file.writeText(VirtualConfig.fromDefinition(response.config).internalToJsonObject().toString())
    }

    suspend inline fun <reified T> updateCustomConfig(name: String, value: T) {
        val response =
            Node.instance.virtualConfigServiceImpl.updateVirtualConfig(
                UpdateVirtualConfigRequest.newBuilder()
                    .setName(name)
                    .setConfig(json.encodeToString(value))
                    .build()
            )
        val file = tempConfigsPath.resolve("$name.json").toFile()
        file.parentFile.mkdirs()
        file.writeText(VirtualConfig.fromDefinition(response.config).internalToJsonObject().toString())
    }

    suspend fun updateCustomConfig(config: VirtualConfig) {
        val response =
            Node.instance.virtualConfigServiceImpl.updateVirtualConfig(
                updateVirtualConfigRequest {
                    this.config = config.config.toString()
                    this.name = config.name
                }
            )
        val file = tempConfigsPath.resolve("${config.name}.json").toFile()
        file.parentFile.mkdirs()
        file.writeText(VirtualConfig.fromDefinition(response.config).internalToJsonObject().toString())
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
        file.writeText(newConfig.internalToJsonObject().toString())
    }

    suspend fun updateLocalConfigFromDatabase(config: VirtualConfig) {
        val newConfig = getCustomConfig(config.name, true) ?: return
        val file = tempConfigsPath.resolve("${config.name}.json").toFile()
        file.parentFile.mkdirs()
        file.writeText(newConfig.internalToJsonObject().toString())
    }

    suspend fun updateDatabaseFromLocalConfig(name: String) {
        val json = getLocalConfigJson(name) ?: return
        Node.instance.localGrpcClient.virtualConfigAPI.updateVirtualConfig(
            updateVirtualConfigRequest {
                this.config = json
                this.name = name
            }
        )
    }

    suspend fun updateDatabaseFromLocalConfig(config: VirtualConfig) {
        val json = getLocalConfigJson(config.name) ?: return
        Node.instance.localGrpcClient.virtualConfigAPI.updateVirtualConfig(
            updateVirtualConfigRequest {
                this.config = json
                this.name = config.name
            }
        )
    }

    fun deleteLocalConfig(name: String) {
        val file = tempConfigsPath.resolve("$name.json").toFile()
        if (file.exists()) {
            file.delete()
        }
    }
}