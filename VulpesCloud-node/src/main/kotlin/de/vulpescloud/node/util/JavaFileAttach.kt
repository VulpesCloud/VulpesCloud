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

import de.vulpescloud.node.Node
import lombok.SneakyThrows
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream


object JavaFileAttach {

    @SneakyThrows
    fun append(inputJarPath: File, file: String, pluginDataPath: String?) {
        val pathSeparators = file.split("/".toRegex()).toTypedArray()
        val name = pathSeparators[pathSeparators.size - 1]

        try {
            val tempJarFile = File.createTempFile("tempJar", ".jar")
            tempJarFile.deleteOnExit()

            JarFile(inputJarPath).use { jarFile ->
                FileOutputStream(tempJarFile).use { fos ->
                    JarOutputStream(fos).use { jos ->
                        val entries: Enumeration<JarEntry> = jarFile.entries()
                        while (entries.hasMoreElements()) {
                            val entry: JarEntry = entries.nextElement()
                            jos.putNextEntry(JarEntry(entry.name))
                            jarFile.getInputStream(entry).use { `is` ->
                                val buffer = ByteArray(1024)
                                var length: Int
                                while ((`is`.read(buffer).also { length = it }) > 0) {
                                    jos.write(buffer, 0, length)
                                }
                            }
                            jos.closeEntry()
                        }
                        Node::class.java.getClassLoader().getResourceAsStream("platforms/$file").use { `is` ->
                            jos.putNextEntry(JarEntry((if (pluginDataPath == null) "" else "$pluginDataPath/") + name))
                            val buffer = ByteArray(1024)
                            var length: Int
                            if (`is` != null) {
                                while ((`is`.read(buffer).also { length = it }) > 0) {
                                    jos.write(buffer, 0, length)
                                }
                            }
                            jos.closeEntry()
                        }
                    }
                }
            }
            val outputJarFile = File(inputJarPath.absolutePath)
            if (outputJarFile.exists()) {
                outputJarFile.delete()
            }
            tempJarFile.renameTo(outputJarFile)
        } catch (e: IOException) {
            LoggerFactory.getLogger(JavaFileAttach::class.java)
                .error("A new error has occured in JavaFileAttach! >> $e")
        }
    }

}