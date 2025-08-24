package de.vulpescloud.api.serversoftware

import build.buf.gen.vulpescloud.node.v1.ServerSoftware
import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString

@Serializable
data class ServerSoftware(
    val name: String,
    val version: String,
    val url: String,
    val pluginDir: String,
    val type: SoftwareType,
) {

    fun toDocument(): BsonDocument =
        BsonDocument().apply {
            append("name", BsonString(name))
            append("version", BsonString(version))
            append("url", BsonString(url))
            append("pluginDir", BsonString(pluginDir))
            append("type", BsonInt32(type.ordinal))
        }

    fun toDefinition(): ServerSoftware {
        return ServerSoftware.newBuilder()
            .setName(name)
            .setVersion(version)
            .setUrl(url)
            .setPluginDir(pluginDir)
            .setType(
                when (type) {
                    SoftwareType.SERVER ->
                        (build.buf.gen.vulpescloud.node.v1.SoftwareType.SOFTWARE_TYPE_SERVER)
                    SoftwareType.PROXY ->
                        (build.buf.gen.vulpescloud.node.v1.SoftwareType.SOFTWARE_TYPE_PROXY)
                }
            )
            .build()
    }

    companion object {
        fun fromDefinition(
            definition: ServerSoftware
        ): de.vulpescloud.api.serversoftware.ServerSoftware {
            return ServerSoftware(
                definition.name,
                definition.version,
                definition.url,
                definition.pluginDir,
                when (definition.type) {
                    build.buf.gen.vulpescloud.node.v1.SoftwareType.SOFTWARE_TYPE_SERVER ->
                        SoftwareType.SERVER
                    build.buf.gen.vulpescloud.node.v1.SoftwareType.SOFTWARE_TYPE_PROXY ->
                        SoftwareType.PROXY
                    else -> SoftwareType.SERVER
                },
            )
        }

        fun fromDocument(document: BsonDocument): de.vulpescloud.api.serversoftware.ServerSoftware =
            ServerSoftware(
                document.getString("name").value,
                document.getString("version").value,
                document.getString("url").value,
                document.getString("pluginDir").value,
                SoftwareType.entries[document.getInt32("type").value],
            )
    }
}
