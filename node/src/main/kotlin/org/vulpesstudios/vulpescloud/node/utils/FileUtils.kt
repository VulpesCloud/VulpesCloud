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

package org.vulpesstudios.vulpescloud.node.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object FileUtils {

    fun copyDir(source: Path, target: Path) {
        Files.createDirectories(target)
        Files.walk(source).forEach { sourcePath ->
            val targetPath = target.resolve(source.relativize(sourcePath))
            if (Files.isDirectory(sourcePath)) {
                Files.createDirectories(targetPath)
            } else {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    fun deleteDir(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
    }

    /**
     * Resolves `relative` against `root`, making sure the resulting path can never escape
     * `root` (e.g. via `..` segments or an absolute path). Throws [IllegalArgumentException] if
     * it would.
     */
    fun resolveSafe(root: Path, relative: String): Path {
        val normalizedRoot = root.toAbsolutePath().normalize()
        val cleaned = relative.trim().trimStart('/', '\\')
        val target = normalizedRoot.resolve(cleaned).normalize()

        if (target != normalizedRoot && !target.startsWith(normalizedRoot)) {
            throw IllegalArgumentException("Path '$relative' escapes its template root")
        }

        return target
    }

    fun guessMimeType(path: Path): String =
        runCatching { Files.probeContentType(path) }.getOrNull() ?: "application/octet-stream"
}
