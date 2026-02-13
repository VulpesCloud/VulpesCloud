package de.vulpescloud.api.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.json.JSONObject

object JSONObjectSerializer : KSerializer<JSONObject> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("JSONObject", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): JSONObject {
        return JSONObject(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: JSONObject) {
        encoder.encodeString(value.toString())
    }
}
