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



package org.vulpesstudios.vulpescloud.launcher.dependency

import org.json.JSONObject
import org.vulpesstudios.vulpescloud.launcher.VulpesLauncher
import java.io.*
import java.net.URI
import java.nio.file.Path
import java.util.jar.JarFile

class DependencyDownloader {
    private val DOWNLOAD_DIR = Path.of("launcher/dependencies/maven")

    fun downloadDependencies() {
        val jarFile = JarFile(File("launcher/dependencies/vulpescloud/vulpescloud-node.jar"))
        val dependenciesJson = jarFile.getJarEntry("dependencies.json")

        if (dependenciesJson == null) {
            System.err.println("No dependencies.json found in the vulpescloud-node.jar")
            return
        }

        val reader = InputStreamReader(jarFile.getInputStream(dependenciesJson))
        val json = JSONObject(reader.readText())

        val jsonArray = json.getJSONArray("dependencies")

        for (i in 0 until jsonArray.length()) {
            jsonArray.getJSONObject(i).let { json ->
                val url = json.getString("url")
                val artifact = json.getString("artifact")
                val version = json.getString("version")

                this.DOWNLOAD_DIR.toFile().mkdirs()
                val file = this.DOWNLOAD_DIR.resolve("$artifact-$version.jar").toFile()

                if (!file.exists()) {
                    println("Downloading dependency: $artifact-$version.jar")
                    this.download(url, file)
                }
                VulpesLauncher.CLASS_LOADER.addURL(file.toURI().toURL())
            }
        }
    }

    private fun download(url: String, file: File) {
        try {
            URI(url).toURL().openStream().use { inputStream ->
                BufferedOutputStream(FileOutputStream(file.toString())).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}