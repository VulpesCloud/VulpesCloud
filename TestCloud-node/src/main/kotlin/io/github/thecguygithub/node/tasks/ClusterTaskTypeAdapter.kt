package io.github.thecguygithub.node.tasks

import com.google.gson.*
import io.github.thecguygithub.api.platforms.PlatformTypes
import io.github.thecguygithub.api.tasks.ClusterTask
import io.github.thecguygithub.api.tasks.FallbackClusterTask
import io.github.thecguygithub.node.platforms.versions.PlatformVersion
import io.github.thecguygithub.node.platforms.versions.PlatformVersionTypeAdapter
import org.jetbrains.annotations.Nullable

import java.lang.reflect.Type;


class ClusterTaskTypeAdapter : JsonDeserializer<ClusterTask?>,
    JsonSerializer<FallbackClusterTask?> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): ClusterTask {
        val group: ClusterTask = context.deserialize(
            json,
            ClusterTaskImpl::class.java
        ) as ClusterTask

        val `object` = json as JsonObject

        if (json.has("fallback") && `object`["fallback"].asBoolean) {
            return ClusterTaskFallbackImpl(group)
        }
        return group
    }

    @Nullable
    override fun serialize(
        p0: FallbackClusterTask?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        val group = SILENT_GSON.toJsonTree(p0) as JsonObject

        if (p0 != null) {
            if (p0.platform()?.type == PlatformTypes.SERVER) {
                group.addProperty("fallback", true)
            }
        }

        return group
    }

    companion object {
        private val SILENT_GSON: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(
            PlatformVersion::class.java, PlatformVersionTypeAdapter()
        ).create()
    }
}