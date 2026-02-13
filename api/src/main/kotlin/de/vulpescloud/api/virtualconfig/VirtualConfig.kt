package de.vulpescloud.api.virtualconfig

import de.vulpescloud.api.serializer.JSONObjectSerializer
import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.bson.BsonString
import org.json.JSONObject

@Serializable
data class VirtualConfig(
    val name: String,
    val createdAt: Long,
    val lastUpdatedAt: Long,
    @Serializable(JSONObjectSerializer::class) val config: JSONObject,
) {

    fun toDefinition(): build.buf.gen.vulpescloud.virtualconfig.v1.VirtualConfig {
        return build.buf.gen.vulpescloud.virtualconfig.v1.VirtualConfig.newBuilder()
            .setName(name)
            .setCreatedAt(createdAt)
            .setLastUpdatedAt(lastUpdatedAt)
            .setConfig(config.toString(4))
            .build()
    }

    fun toDocument(): BsonDocument {
        return BsonDocument().apply {
            put("name", BsonString(name))
            put("createdAt", BsonInt64(createdAt))
            put("lastUpdatedAt", BsonInt64(lastUpdatedAt))
            put("config", BsonString(config.toString(4)))
        }
    }

    companion object {
        fun fromDocument(document: BsonDocument): VirtualConfig {
            return VirtualConfig(
                document.getString("name").value,
                document.getInt64("createdAt").value,
                document.getInt64("lastUpdatedAt").value,
                JSONObject(document.getString("config").value),
            )
        }

        fun fromDefinition(
            definition: build.buf.gen.vulpescloud.virtualconfig.v1.VirtualConfig
        ): VirtualConfig {
            return VirtualConfig(
                definition.name,
                definition.createdAt,
                definition.lastUpdatedAt,
                JSONObject(definition.config.toString()),
            )
        }
    }
}
