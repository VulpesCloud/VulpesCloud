package de.vulpescloud.launcher.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*

object FileSystemUtil {

    fun copyClassPathFile(classLoader: ClassLoader, fileName: String, path: String) {
        val target = Path.of(path)

        // create dir if not exists
        target.parent.toFile().mkdirs()

        Files.copy(
            Objects.requireNonNull(classLoader.getResourceAsStream(fileName)),
            target,
            StandardCopyOption.REPLACE_EXISTING
        )
    }

}