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

package de.vulpescloud.node.util

import lombok.SneakyThrows
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption


object DirectoryActions {

    @SneakyThrows
    private fun deleteDirectoryContents(directoryPath: File): Boolean {
        if (!directoryPath.exists() || !directoryPath.isDirectory) {
            return false
        }

        val files = directoryPath.listFiles() ?: return false

        var success = true
        for (file in files) {
            success = success and deleteRecursively(file)
        }
        Files.deleteIfExists(directoryPath.toPath())
        return success
    }

    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            val files = file.listFiles()
            if (files != null) {
                for (child in files) {
                    if (!deleteRecursively(child)) {
                        return false
                    }
                }
            }
        }
        return file.delete()
    }

    fun delete(file: File) {
        deleteDirectoryContents(file)
    }

    fun delete(file: Path): Boolean {
        return deleteDirectoryContents(file.toFile())
    }

    @SneakyThrows
    fun createDirectory(path: Path): Path {
        if (!Files.exists(path)) {
            createDirectory(path)
        }
        return path
    }

    fun createDirectory(path: String?): Path {
        return createDirectory(Path.of(path))
    }

    fun copyDirectoryContents(sourceDirectoryPath: Path, targetDirectoryPath: Path): Boolean {
        return copyDirectoryContents(sourceDirectoryPath.toString(), targetDirectoryPath.toString())
    }

    fun copyDirectoryContents(sourceDirectoryPath: String?, targetDirectoryPath: String?): Boolean {
        val sourceDirectory = File(sourceDirectoryPath)
        val targetDirectory = File(targetDirectoryPath)

        if (!sourceDirectory.exists() || !sourceDirectory.isDirectory) {
            return false
        }

        if (!targetDirectory.exists()) {
            if (!targetDirectory.mkdirs()) {
                return false
            }
        }

        val files = sourceDirectory.listFiles() ?: return false

        var success = true
        for (file in files) {
            val targetFile = File(targetDirectory, file.name)
            success = if (file.isDirectory) {
                success and copyDirectoryContents(file.absolutePath, targetFile.absolutePath)
            } else {
                success and copyFile(file.toPath(), targetFile.toPath())
            }
        }
        return success
    }

    private fun copyFile(source: Path, target: Path): Boolean {
        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }


}