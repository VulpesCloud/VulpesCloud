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

package de.vulpescloud.launcher.dependency

import de.vulpescloud.launcher.VulpesLauncher
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.nio.file.Path
import java.util.jar.JarFile

class DependencyDownloader {
    private val DOWNLOAD_DIR = Path.of("launcher/dependencies")

    fun downloadDependency() {
        val jarFile = JarFile(File("launcher/dependencies/vulpescloud-node.jar"))
        val dependenciesJson = jarFile.getJarEntry("dependencies.json")

        if (dependenciesJson == null) {
            System.err.println("No dependencies.json found in the vulpescloud-node.jar")
            return
        }

        val reader = InputStreamReader(jarFile.getInputStream(dependenciesJson))
        val json = JSONObject(reader.readText())

        val jsonArray = JSONObject(json).getJSONArray("dependencies")

        for (i in 0 until jsonArray.length()) {
            jsonArray.getJSONObject(i).let { json ->
                val url = json.getString("url")
                val artifact = json.getString("artifact")
                val version = json.getString("version")

                this.DOWNLOAD_DIR.toFile().mkdirs()
                val file = this.DOWNLOAD_DIR.resolve("$artifact-$version.jar").toFile()

                if (!file.exists()) {
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