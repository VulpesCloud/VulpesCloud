package de.vulpescloud.api.metrics

import build.buf.gen.vulpescloud.modules.metrics.v1.FieldValue
import build.buf.gen.vulpescloud.modules.metrics.v1.Point
import build.buf.gen.vulpescloud.modules.metrics.v1.fieldValue
import build.buf.gen.vulpescloud.modules.metrics.v1.point

fun buildPoint(
    measurement: String,
    timestampNanos: Long = System.currentTimeMillis() * 1_000_000L,
    block: PointBuilder.() -> Unit,
): Point = PointBuilder(measurement, timestampNanos).apply(block).build()

class PointBuilder(private val measurement: String, private val ts: Long) {
    private val tags = mutableMapOf<String, String>()
    private val fields = mutableMapOf<String, FieldValue>()

    fun tag(key: String, value: String) {
        tags[key] = value
    }

    fun field(key: String, value: Double) {
        fields[key] = fieldValue { doubleValue = value }
    }

    fun field(key: String, value: Long) {
        fields[key] = fieldValue { intValue = value }
    }

    fun field(key: String, value: String) {
        fields[key] = fieldValue { stringValue = value }
    }

    fun field(key: String, value: Boolean) {
        fields[key] = fieldValue { boolValue = value }
    }

    fun build(): Point = point {
        measurementName = measurement
        tags.putAll(this@PointBuilder.tags)
        fields.putAll(this@PointBuilder.fields)
        timestamp = ts
    }
}
