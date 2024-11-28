package de.vulpescloud.launcher.dependency

import de.vulpescloud.launcher.VulpesLauncher
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.nio.file.Path

class DependencyDownloader {
    private val DOWNLOAD_DIR = Path.of("launcher/dependencies")

    private fun download(dependency: Dependency) {
        this.DOWNLOAD_DIR.toFile().mkdirs()
        val file = this.DOWNLOAD_DIR.resolve("$dependency.jar").toFile()

        if (!file.exists()) {
            this.download(dependency.downloadUrl(), file)
        }
        VulpesLauncher.CLASS_LOADER.addURL(file.toURI().toURL())
    }

    fun download(vararg dependencies: Dependency) {
        downloadDependenciesWithProgress(listOf(*dependencies))
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

    private fun downloadDependenciesWithProgress(dependencies: List<Dependency>) {
        val totalDependencies = dependencies.size
        for (i in 0 until totalDependencies) {
            val dependency = dependencies[i]
            logProgress(totalDependencies, i + 1, dependency)
            this.download(dependency)
        }

        clearTerminal()
    }

    private fun logProgress(total: Int, current: Int, dependency: Dependency) {
        if (findDependency(dependency).exists()) {
            return
        }
        System.out.printf("Downloading Dependency - %s %d %d \n", dependency.artifactId, current, total)
    }

    private fun clearTerminal() {
        defaultSys("\r", " ".repeat(80), "\r")
    }

    private fun defaultSys(vararg messages: String) {
        for (message in messages) {
            print(message)
        }
    }

    private fun findDependency(dependency: Dependency): File {
        return this.DOWNLOAD_DIR.resolve("$dependency.jar").toFile()
    }

}