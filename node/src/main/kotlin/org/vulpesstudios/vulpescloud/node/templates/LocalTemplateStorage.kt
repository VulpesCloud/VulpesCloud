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

package org.vulpesstudios.vulpescloud.node.templates

import build.buf.gen.vulpescloud.templates.v1.TemplateStorageType
import kotlinx.coroutines.runBlocking
import org.vulpesstudios.vulpescloud.api.templates.Template
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.utils.FileUtils
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.Path

class LocalTemplateStorage : TemplateStorage {
    override fun name(): String = "LOCAL"

    override fun type(): TemplateStorageType =
        TemplateStorageType.newBuilder().setName("LOCAL").build()

    override fun nodeName(): String = Node.instance.configProvider.config.nodeName

    private val templatesPath = Path("local/templates")
    private val templateStagingPath = Path("local/template-staging")

    init {
        templatesPath.toFile().mkdirs()
        FileUtils.deleteDir(templateStagingPath)
        Files.createDirectories(templateStagingPath)
    }

    private fun rootOf(template: Template): Path = templatesPath.resolve(template.id)

    override fun copyTemplateToPath(template: Template, path: Path) {
        val templatePath = rootOf(template)
        templatePath.toFile().mkdirs()
        FileUtils.copyDir(templatePath, path)
    }

    override fun copyPathToTemplate(path: Path, template: Template) {
        val templatePath = rootOf(template)
        templatePath.toFile().mkdirs()
        FileUtils.copyDir(path, templatePath)
    }

    override fun deleteTemplate(template: Template) {
        FileUtils.deleteDir(rootOf(template))
    }

    override fun createTemplate(template: Template) {
        rootOf(template).toFile().mkdirs()
    }

    override fun hasTemplate(template: Template): Boolean {
        return Files.exists(rootOf(template))
    }

    private suspend fun fillTemplateData(id: String): Template {
        return TemplateRegistry.getById(id)!!
    }

    override fun templates(): List<Template> {
        return runBlocking {
            templatesPath
                .toFile()
                .listFiles()
                ?.filter { it.isDirectory }
                ?.map { fillTemplateData(it.name) } ?: emptyList()
        }
    }

    override fun createDirectory(template: Template, path: String) {
        val target = FileUtils.resolveSafe(rootOf(template), path)
        Files.createDirectories(target)
    }

    override fun deleteDirectory(template: Template, path: String, recursive: Boolean) {
        val target = FileUtils.resolveSafe(rootOf(template), path)
        if (!Files.exists(target)) return
        require(Files.isDirectory(target)) { "'$path' is not a directory" }

        if (!recursive && Files.list(target).use { it.findAny().isPresent }) {
            throw IllegalStateException("Directory '$path' is not empty")
        }

        FileUtils.deleteDir(target)
    }

    override fun createFile(template: Template, path: String, content: ByteArray) {
        val target = FileUtils.resolveSafe(rootOf(template), path)
        require(!Files.exists(target)) { "File '$path' already exists" }
        Files.createDirectories(target.parent)
        Files.write(target, content)
    }

    override fun updateFile(template: Template, path: String, content: ByteArray) {
        val target = FileUtils.resolveSafe(rootOf(template), path)
        Files.createDirectories(target.parent)
        Files.write(target, content)
    }

    override fun deleteFile(template: Template, path: String) {
        val target = FileUtils.resolveSafe(rootOf(template), path)
        require(Files.exists(target)) { "File '$path' does not exist" }
        require(!Files.isDirectory(target)) { "'$path' is a directory, not a file" }
        Files.delete(target)
    }

    override fun readFile(template: Template, path: String): TemplateFileData {
        val target = FileUtils.resolveSafe(rootOf(template), path)
        require(Files.exists(target) && !Files.isDirectory(target)) {
            "File '$path' does not exist"
        }

        return TemplateFileData(
            path = path,
            content = Files.readAllBytes(target),
            size = Files.size(target),
            mimeType = FileUtils.guessMimeType(target),
            modifiedAt = Files.getLastModifiedTime(target).toMillis(),
        )
    }

    override fun openFileRead(template: Template, path: String): TemplateFileReadSession {
        val target = FileUtils.resolveSafe(rootOf(template), path)
        require(Files.exists(target) && !Files.isDirectory(target)) { "File '$path' does not exist" }
        return object : TemplateFileReadSession {
            override val totalSize: Long = Files.size(target)
            override val modifiedAt: Long = Files.getLastModifiedTime(target).toMillis()
            private val input = Files.newInputStream(target)
            override fun read(buffer: ByteArray): Int = input.read(buffer)
            override fun close() = input.close()
        }
    }

    override fun beginFileWrite(
        template: Template,
        path: String,
        totalSize: Long,
        modifiedAt: Long,
    ): TemplateFileWriteSession {
        require(totalSize >= 0) { "File size cannot be negative" }
        val target = FileUtils.resolveSafe(rootOf(template), path)
        val staging = Files.createTempFile(templateStagingPath, "template-", ".part")
        var output: OutputStream? = Files.newOutputStream(staging)
        var received = 0L
        var finished = false

        return object : TemplateFileWriteSession {
            override fun write(content: ByteArray, offset: Int, length: Int) {
                check(!finished) { "File write session is already finished" }
                require(offset >= 0 && length >= 0 && offset + length <= content.size) { "Invalid chunk range" }
                check(received + length <= totalSize) { "Received more than the declared file size" }
                output!!.write(content, offset, length)
                received += length
            }

            override fun commit() {
                check(!finished) { "File write session is already finished" }
                check(received == totalSize) { "Received $received bytes but expected $totalSize" }
                output!!.close()
                output = null
                Files.createDirectories(target.parent)
                try {
                    Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING)
                }
                finished = true
            }

            override fun abort() {
                if (finished) return
                finished = true
                runCatching { output?.close() }
                output = null
                Files.deleteIfExists(staging)
            }

            override fun close() = abort()
        }
    }

    override fun listDirectory(
        template: Template,
        path: String,
    ): List<TemplateDirectoryEntryData> {
        val root = rootOf(template).toAbsolutePath().normalize()
        val target = FileUtils.resolveSafe(root, path)

        if (!Files.exists(target)) return emptyList()
        require(Files.isDirectory(target)) { "'$path' is not a directory" }

        return Files.list(target).use { stream ->
            stream
                .map { entry ->
                    TemplateDirectoryEntryData(
                        name = entry.fileName.toString(),
                        path = root.relativize(entry).toString().replace('\\', '/'),
                        directory = Files.isDirectory(entry),
                        size = if (Files.isDirectory(entry)) 0 else Files.size(entry),
                        modifiedAt = Files.getLastModifiedTime(entry).toMillis(),
                    )
                }
                .toList()
        }
    }
}
