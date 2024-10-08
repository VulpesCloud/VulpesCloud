package io.github.thecguygithub.node.platforms.versions

import com.google.gson.*

import java.lang.reflect.Type;


class PlatformVersionTypeAdapter : JsonSerializer<PlatformVersion?>,
    JsonDeserializer<PlatformVersion?> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): PlatformVersion? {
        val `object` = json.asJsonObject
        val version = `object`["version"].asString

        if (`object`.has("fileName")) {
            return PlatformPathVersion(version, `object`["fileName"].asString)
        }
        if (`object`.has("url")) {
            return PlatformUrlVersion(version, `object`["url"].asString)
        }
        return null
    }

    override fun serialize(p0: PlatformVersion?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement? {
        val `object` = JsonObject()

        `object`.addProperty("version", p0?.version)

        if (p0 is PlatformPathVersion) {
            `object`.addProperty("fileName", p0.fileName)
        } else if (p0 is PlatformUrlVersion) {
            `object`.addProperty("url", p0.url)
        }

        return `object`
    }
}