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

package org.vulpesstudios.vulpescloud.launcher.util

import org.json.JSONObject
import org.vulpesstudios.vulpescloud.launcher.VulpesLauncher.Companion.GITHUBURL
import org.vulpesstudios.vulpescloud.launcher.VulpesLauncher.Companion.config
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.io.path.Path

object ChecksumUtil {

    fun getFileChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun returnChecksumJson(): JSONObject {
        return JSONObject(Files.readString(Path("launcher/checksums.json")))
    }

    fun downloadChecksumJson() {
        Files.writeString(
            Path("launcher/checksums.json"),
            String(
                URI(GITHUBURL + config.autoUpdatesBranch() + "/checksums.json")
                .toURL()
                .openStream()
                .readAllBytes()
            )
        )
    }

}