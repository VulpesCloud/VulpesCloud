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

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration


object FileUpdaterUtil {

    fun <T> get(uri: URI, body: BodyHandler<T>): HttpResponse<T> {
        val request = HttpRequest.newBuilder()
            .GET()
            .uri(uri)
            .timeout(Duration.ofMinutes(1))
            .header("user-agent", ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/97.0.4692.56 Safari/537.36"))
            .build()

        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build()
            .send(request, body)
    }

    fun filePathHandler(filePath: Path): BodyHandler<Path> {
        return HttpResponse.BodyHandlers.ofFile(
            filePath,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }

    fun updateFile(target: File, downloadURI: URI, downloadChecksum: String): Boolean {
        if (Files.exists(target.toPath())) {
            val currentChecksum = ChecksumUtil.getFileChecksum(target)
            if (currentChecksum == downloadChecksum) {
                println("File ${target.name} is already up to date!")
                return false
            }
        }

        println("Updating ${target.name}")

        this.get(downloadURI, filePathHandler(target.toPath()))

        val newChecksum = ChecksumUtil.getFileChecksum(target)

        if (newChecksum != downloadChecksum) {
            throw IllegalStateException("Checksum mismatch for file ${target.name}")
        }

        return true
    }

}