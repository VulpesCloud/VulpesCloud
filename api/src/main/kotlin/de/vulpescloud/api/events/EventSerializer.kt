package de.vulpescloud.api.events

import build.buf.gen.vulpescloud.events.v1.Event
import kotlinx.serialization.json.Json
import java.util.*

object EventSerializer {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    inline fun <reified T> encode(event: T, metadata: Map<String, String> = emptyMap()): Event {
        val payload = json.encodeToString(event).toByteArray()
        return Event.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setType(T::class.qualifiedName ?: "Unknown")
            .putAllMetadata(metadata)
            .setPayload(com.google.protobuf.ByteString.copyFrom(payload))
            .setTimestamp(System.currentTimeMillis())
            .build()
    }

    inline fun <reified T> decode(event: Event): T {
        val payloadJson = event.payload.toStringUtf8()
        return json.decodeFromString(payloadJson)
    }
}
