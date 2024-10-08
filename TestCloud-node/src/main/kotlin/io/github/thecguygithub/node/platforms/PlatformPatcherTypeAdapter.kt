package io.github.thecguygithub.node.platforms

import com.google.gson.*
import io.github.thecguygithub.node.platforms.patcher.PlatformPatcher
import io.github.thecguygithub.node.platforms.patcher.PlatformPatcherPool.patcher
import org.jetbrains.annotations.Contract
import java.lang.reflect.Type


class PlatformPatcherTypeAdapter : JsonSerializer<PlatformPatcher?>,
    JsonDeserializer<PlatformPatcher?> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): PlatformPatcher? {
        return patcher(json.asString)
    }

    @Contract("_, _, _ -> new")
    override fun serialize(p0: PlatformPatcher?, p1: Type?, context: JsonSerializationContext?): JsonPrimitive {
        return JsonPrimitive(p0?.id())
    }
}