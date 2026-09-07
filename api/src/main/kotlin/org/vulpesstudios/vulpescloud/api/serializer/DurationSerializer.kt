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

package org.vulpesstudios.vulpescloud.api.serializer

import com.google.protobuf.Duration
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Duration", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeString("${value.seconds}s${if (value.nanos != 0) "+${value.nanos}ns" else ""}")
    }

    override fun deserialize(decoder: Decoder): Duration {
        val str = decoder.decodeString()
        return try {
            if (str.contains("s")) {
                val parts = str.split("+")
                val secondsStr = parts[0].removeSuffix("s")
                val seconds = secondsStr.toLong()
                val nanos = if (parts.size > 1 && parts[1].endsWith("ns")) {
                    parts[1].removeSuffix("ns").toInt()
                } else {
                    0
                }
                Duration.newBuilder().setSeconds(seconds).setNanos(nanos).build()
            } else {
                val seconds = str.toLong()
                Duration.newBuilder().setSeconds(seconds).build()
            }
        } catch (e: Exception) {
            Duration.getDefaultInstance()
        }
    }
}
