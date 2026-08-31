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

package org.vulpesstudios.vulpescloud.api.serversoftware

import build.buf.gen.vulpescloud.node.v1.ServerSoftware
import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString
import build.buf.gen.vulpescloud.node.v1.SoftwareType as SoftwareTypeProto

@Serializable
data class ServerSoftware(
    val name: String,
    val version: String,
    val build: Int,
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
            append("build", BsonInt32(build))
        }

    fun toDefinition(): ServerSoftware {
        return ServerSoftware.newBuilder()
            .setName(name)
            .setVersion(version)
            .setUrl(url)
            .setPluginDir(pluginDir)
            .setType(
                when (type) {
                    SoftwareType.SERVER -> SoftwareTypeProto.SOFTWARE_TYPE_SERVER
                    SoftwareType.PROXY -> SoftwareTypeProto.SOFTWARE_TYPE_PROXY
                }
            )
            .setBuild(build)
            .build()
    }

    companion object {
        fun fromDefinition(
            definition: ServerSoftware
        ): org.vulpesstudios.vulpescloud.api.serversoftware.ServerSoftware {
            return ServerSoftware(
                definition.name,
                definition.version,
                definition.build,
                definition.url,
                definition.pluginDir,
                when (definition.type) {
                    SoftwareTypeProto.SOFTWARE_TYPE_SERVER -> SoftwareType.SERVER
                    SoftwareTypeProto.SOFTWARE_TYPE_PROXY -> SoftwareType.PROXY
                    else -> SoftwareType.SERVER
                },
            )
        }

        fun fromDocument(document: BsonDocument): org.vulpesstudios.vulpescloud.api.serversoftware.ServerSoftware =
            ServerSoftware(
                document.getString("name").value,
                document.getString("version").value,
                document.getInt32("build").value,
                document.getString("url").value,
                document.getString("pluginDir").value,
                SoftwareType.entries[document.getInt32("type").value],
            )
    }
}
