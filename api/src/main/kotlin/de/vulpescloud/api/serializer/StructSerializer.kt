package de.vulpescloud.api.serializer

import com.google.protobuf.Struct
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object StructSerializer : KSerializer<Struct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Struct", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Struct) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Struct {
        return Struct.parseFrom(decoder.decodeString().toByteArray())
    }
}
