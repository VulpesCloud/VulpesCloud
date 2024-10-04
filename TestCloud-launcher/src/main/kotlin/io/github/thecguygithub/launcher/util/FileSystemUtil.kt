package io.github.thecguygithub.launcher.util

import lombok.SneakyThrows
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*


object FileSystemUtil {

    @SneakyThrows
    fun copyClassPathFile(classLoader: ClassLoader, fileName: String?, path: String?) {
        println(classLoader)
        println(fileName)
        println(path)


        val target: Path
        if (path != null) {
            target = Path.of(path)
        } else {
            println(path + "is Null!")
            return
        }

        println(target)
        println(classLoader.getResourceAsStream(fileName))


        // create dir if not exists
        target.parent.toFile().mkdirs()

        Files.copy(
            Objects.requireNonNull(classLoader.getResourceAsStream(fileName)) ?: throw IllegalArgumentException("Resource not found: $fileName"),
            target,
            StandardCopyOption.REPLACE_EXISTING
        )
    }

}