package de.vulpescloud.node.utils

import java.nio.file.Files
import java.nio.file.Path

object FileUtils {

    fun copyDir(source: Path, target: Path) {
        Files.createDirectories(target)
        Files.walk(source).forEach { sourcePath ->
            val targetPath = target.resolve(source.relativize(sourcePath))
            if (Files.isDirectory(sourcePath)) {
                Files.createDirectories(targetPath)
            } else {
                Files.copy(sourcePath, targetPath)
            }
        }
    }

    fun deleteDir(path: Path) {
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.delete(it) }
    }


}
