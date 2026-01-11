package de.vulpescloud.bridge.impl.virtualconfig

import build.buf.gen.vulpescloud.virtualconfig.v1.VirtualConfigServiceGrpcKt
import build.buf.gen.vulpescloud.virtualconfig.v1.configOrNull
import build.buf.gen.vulpescloud.virtualconfig.v1.getByNameRequest
import build.buf.gen.vulpescloud.virtualconfig.v1.updateVirtualConfigRequest
import de.vulpescloud.api.virtualconfig.VirtualConfig
import de.vulpescloud.bridge.VirtualConfigAPI
import de.vulpescloud.wrapper.Wrapper
import de.vulpescloud.wrapper.grpc.AuthClientInterceptor
import java.nio.file.Path
import kotlin.io.path.Path
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.json.JSONObject

class VirtualConfigCoroutineAPIImpl : VirtualConfigAPI.VirtualConfigCoroutineAPI {

    val stub =
        VirtualConfigServiceGrpcKt.VirtualConfigServiceCoroutineStub(
                Wrapper.instance.grpcClient.channel
            )
            .withInterceptors(AuthClientInterceptor(System.getenv("secret")))

    val tempConfigsPath: Path = Path("temp").resolve("configs")

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        isLenient = true
    }

    override suspend fun <T> getCustomConfigObject(
        name: String,
        serializer: KSerializer<T>,
        forceGet: Boolean,
    ): T? {
        val file = tempConfigsPath.resolve("$name.json").toFile()
        if (!forceGet) {
            if (file.exists()) {
                getLocalConfigJson(name)?.let {
                    return json.decodeFromString(serializer, it)
                }
            }
        }
        val config = stub.getByName(getByNameRequest { this.name = name }).configOrNull

        if (config == null) {
            return null
        }

        file.parentFile.mkdirs()
        file.writeText(JSONObject(VirtualConfig.fromDefinition(config)).toString())
        return json.decodeFromString(serializer, config.config ?: return null)
    }

    override suspend fun getCustomConfig(name: String, forceGet: Boolean): VirtualConfig? {
        val file = tempConfigsPath.resolve("$name.json").toFile()
        if (!forceGet) {
            if (file.exists()) {
                return getLocalConfig(name)
            }
        }
        val config = stub.getByName(getByNameRequest { this.name = name }).configOrNull

        if (config == null) {
            return null
        }

        file.parentFile.mkdirs()
        file.writeText(JSONObject(VirtualConfig.fromDefinition(config)).toString())
        return VirtualConfig.fromDefinition(config)
    }

    override suspend fun <T> updateCustomConfig(
        name: String,
        serializer: KSerializer<T>,
        value: T,
    ) {
        val response =
            stub.updateVirtualConfig(
                updateVirtualConfigRequest {
                    this.config = json.encodeToString(serializer, value)
                    this.name = name
                }
            )
        val file = tempConfigsPath.resolve("$name.json").toFile()
        file.parentFile.mkdirs()
        file.writeText(JSONObject(VirtualConfig.fromDefinition(response.config)).toString())
    }

    override suspend fun updateCustomConfig(config: VirtualConfig) {
        val response =
            stub.updateVirtualConfig(
                updateVirtualConfigRequest {
                    this.config = config.config.toString()
                    this.name = config.name
                }
            )
        val file = tempConfigsPath.resolve("${config.name}.json").toFile()
        file.parentFile.mkdirs()
        file.writeText(JSONObject(VirtualConfig.fromDefinition(response.config)).toString())
    }

    override suspend fun updateLocalConfigFromDatabase(name: String) {
        val newConfig = getCustomConfig(name, true) ?: return
        val file = tempConfigsPath.resolve("${name}.json").toFile()
        file.parentFile.mkdirs()
        file.writeText(JSONObject(newConfig).toString())
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
}
