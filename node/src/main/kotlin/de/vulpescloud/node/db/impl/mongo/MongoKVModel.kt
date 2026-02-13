package de.vulpescloud.node.db.impl.mongo

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MongoKVModel(
    val key: String,
    val value: JsonElement
)
