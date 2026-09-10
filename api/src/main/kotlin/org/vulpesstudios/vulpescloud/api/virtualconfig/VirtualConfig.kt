/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.api.virtualconfig

import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.bson.BsonString
import org.json.JSONObject
import org.vulpesstudios.vulpescloud.api.serializer.JSONObjectSerializer

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
