package de.vulpescloud.node.util

import lombok.SneakyThrows
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path


object Downloader {

    @SneakyThrows
    fun download(link: String, path: Path?) {
        Files.copy(urlStream(link), path)
    }

    @SneakyThrows
    private fun urlStream(link: String): InputStream {
        return URI.create(link).toURL().openConnection().getInputStream()
    }

}