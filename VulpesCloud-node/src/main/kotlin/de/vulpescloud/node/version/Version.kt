/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.node.version

import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.api.version.Versions
import de.vulpescloud.node.service.LocalService
import de.vulpescloud.node.util.DirectoryActions
import de.vulpescloud.node.util.Downloader
import de.vulpescloud.node.version.files.VersionFile
import de.vulpescloud.node.version.patcher.VersionPatcher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class Version(
    val name: String,
    val type: VersionType,
    val pluginDir: String,
    private val patchers: List<VersionPatcher>,
    val files: List<VersionFile>,
    val versions: List<Versions>,
) {

    private val logger: Logger = LoggerFactory.getLogger(Versions::class.java)

    fun prepare(display: VersionInfo, service: LocalService) {
        val version = service.task.version()

        // download only if not exists
        this.download(display, service)

        // copy platform jar and maybe patch files
        DirectoryActions.copyDirectoryContents(
            Path.of("local/versionCache/${version.name}/${version.versions}"),
            service.runningDir
        )
    }

    private fun download(display: VersionInfo, localService: LocalService) {
        val version = versions.firstOrNull { it.version.equals(display.versions, ignoreCase = true) }
            ?: throw NoSuchElementException()

        val platformDir = Path.of("local/versionCache/${display.name}/${display.versions}")

        if (!Files.exists(platformDir)) {
            platformDir.toFile().mkdirs()
        }

        val file = platformDir.resolve("${display.name}-${display.versions}.jar")
        if (!Files.exists(file)) {
            logger.error("Downloading and patching file!")
            Downloader.download(version.link, file)

            patchers.forEach { it.patch(file.toFile(), localService) }
        }
    }
}