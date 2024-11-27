package de.vulpescloud.node.util

import lombok.SneakyThrows
import java.nio.file.Files
import java.nio.file.Path


object Configurations {

    @SneakyThrows
    fun writeContent(path: Path?, value: Any?) {
        Files.writeString(path, JsonUtils.GSON.toJson(value))
    }

    @SneakyThrows
    fun <T : Any> readContent(path: Path, result: T): T {
        if (!Files.exists(path)) {
            writeContent(path, result)
        }
        val jsonContent = Files.readString(path)
        return JsonUtils.GSON.fromJson(jsonContent, result::class.java) as T
    }

}