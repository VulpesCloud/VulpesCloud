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

package de.vulpescloud.api.language

import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture

class LanguageProvider {

    private lateinit var lang: Languages
    private val logger = LoggerFactory.getLogger(LanguageProvider::class.java)
    private val tempZipFile = Paths.get("launcher/temp/lang.zip")
    private val langDir = Paths.get("launcher/Language")
    private val langJson = JSONObject()

    fun setLang(lang: Languages) {
        this.lang = lang
    }

    fun downloadLangFilesFromURI(uri: URI) {
        logger.warn("Something is trying to use downloadLangFilesFromURI from LanguageProvider! This is NOT YET implemented!!!!!!")
//        CompletableFuture.runAsync {
//            val url = uri.toURL()
//            val targetPath = Paths.get("launcher/temp/lang.zip")
//            val langPath = Paths.get("launcher/Language")
//
//            // Recursively delete the directory and its contents
//            if (Files.exists(langPath)) {
//                Files.walkFileTree(langPath, object : SimpleFileVisitor<Path>() {
//                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
//                        Files.delete(file)
//                        return FileVisitResult.CONTINUE
//                    }
//
//                    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
//                        Files.delete(dir)
//                        return FileVisitResult.CONTINUE
//                    }
//                })
//            }
//
//            Files.createDirectories(langPath)
//
//            url.openStream().use { inputStream: InputStream ->
//                println("Downloading language files")
//                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
//            }
//
//            val zipFile = Paths.get("plugins/CUtils/lang/main.zip")
//            val zip = ZipFile(zipFile.toFile())
//            zip.use {
//                zip.entries().asSequence().forEach { entry ->
//                    println("Extracting ${entry.name}")
//                    // Remove the top-level directory from the path
//                    val entryName = entry.name.substringAfter("/")
//                    if (entryName.isNotEmpty()) {
//                        val entryPath = Paths.get("plugins/CUtils/lang/$entryName")
//                        if (entry.isDirectory) {
//                            Files.createDirectories(entryPath)
//                        } else {
//                            zip.getInputStream(entry).use { input ->
//                                Files.copy(input, entryPath, StandardCopyOption.REPLACE_EXISTING)
//                            }
//                        }
//                    }
//                }
//            }
//            Files.delete(zipFile)
//        }
    }

    fun loadLangFilesFromClassPath() {
        CompletableFuture.runAsync {
            Files.createDirectories(langDir)

            this::class.java.classLoader.getResourceAsStream("lang/de_DE.json")
                ?.let { Files.copy(it, langDir.resolve("de_DE.json"), StandardCopyOption.REPLACE_EXISTING) }
            this::class.java.classLoader.getResourceAsStream("lang/en_US.json")
                ?.let { Files.copy(it, langDir.resolve("en_US.json"), StandardCopyOption.REPLACE_EXISTING) }

            if (lang == Languages.en_US) {
                val en_US = JSONObject(Files.readString(langDir.resolve("en_US.json")))
                val keys = en_US.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    langJson.put(key, en_US.get(key))
                }
            } else if (lang == Languages.de_DE) {
                val de_DE = JSONObject(Files.readString(langDir.resolve("de_DE.json")))
                val keys = de_DE.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    langJson.put(key, de_DE.get(key))
                }
            }
        }
    }

    fun translate(key: String): String {
        return if (langJson.has(key)) {
            langJson.getString(key)
        } else {
            "&cLanguage String not found! Please make sure that u have a lang/${lang.name}.json in ur Project and load the LangFiles!"
        }
    }
}