package io.github.thecguygithub.launcher.dependency

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL


object DependencyHelper {

    fun download(url: String?, file: File) {
        try {
            URL(url).openStream().use { inputStream ->
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