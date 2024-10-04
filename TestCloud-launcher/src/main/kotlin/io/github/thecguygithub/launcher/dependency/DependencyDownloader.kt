package io.github.thecguygithub.launcher.dependency

import io.github.thecguygithub.launcher.Launcher
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

object DependencyDownloader {

    private val DOWNLOAD_DIR: Path = Paths.get("local/dependencies")

    @Throws(Exception::class)
    fun download(dependency: Dependency) {
        DOWNLOAD_DIR.toFile().mkdirs()
        val file = DOWNLOAD_DIR.resolve("${dependency}.jar").toFile()

        if (dependency.withSubDependencies) {
            dependency.loadSubDependencies()
            for (subDependency in dependency.subDependencies) {
                download(subDependency)
            }
        }

        if (!file.exists()) {
            DependencyHelper.download(dependency.downloadUrl(), file)
        }
        Launcher().CLASS_LOADER.addURL(file.toURI().toURL())
    }

    fun download(vararg dependencies: Dependency) {
        downloadDependenciesWithProgress(dependencies.toList())
    }

    private fun downloadDependenciesWithProgress(dependencies: List<Dependency>) {
        val totalDependencies = dependencies.size
        for ((index, dependency) in dependencies.withIndex()) {
            logProgress(totalDependencies, index + 1, dependency)
            download(dependency)
        }

        clearTerminal()
    }

    private fun logProgress(total: Int, current: Int, dependency: Dependency) {
        if (findDependency(dependency).exists()) {
            return
        }
        println("Downloading Dependency - ${dependency.artifactId} $current $total")
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
        return DOWNLOAD_DIR.resolve("${dependency}.jar").toFile()
    }
}