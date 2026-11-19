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
import org.vulpesstudios.vulpescloud.api.templates.Template
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** A single file inside a template, as returned by [TemplateStorage.readFile]. */
data class TemplateFileData(
    val path: String,
    val content: ByteArray,
    val size: Long,
    val mimeType: String,
    val modifiedAt: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TemplateFileData

        if (size != other.size) return false
        if (modifiedAt != other.modifiedAt) return false
        if (path != other.path) return false
        if (!content.contentEquals(other.content)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = size.hashCode()
        result = 31 * result + modifiedAt.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

/** A single entry (file or directory) inside a template directory listing. */
data class TemplateDirectoryEntryData(
    val name: String,
    val path: String,
    val directory: Boolean,
    val size: Long,
    val modifiedAt: Long,
)

interface TemplateStorage {

    fun name(): String
    fun type(): TemplateStorageType
    fun nodeName(): String? = null

    // Whole template (directory tree) operations

    fun copyTemplateToPath(template: Template, path: java.nio.file.Path)

    fun copyPathToTemplate(path: java.nio.file.Path, template: Template)

    fun deleteTemplate(template: Template)

    fun createTemplate(template: Template)

    fun hasTemplate(template: Template): Boolean

    fun templates(): List<Template>

    // File/directory level operations. `path` is always relative to the template root and must
    // never be allowed to escape it (implementations are expected to guard against `..` path
    // traversal).

    fun createDirectory(template: Template, path: String)

    fun deleteDirectory(template: Template, path: String, recursive: Boolean)

    fun createFile(template: Template, path: String, content: ByteArray)

    fun updateFile(template: Template, path: String, content: ByteArray)

    fun deleteFile(template: Template, path: String)

    fun readFile(template: Template, path: String): TemplateFileData

    /** Opens a bounded-memory stream for a file. Providers should override this for large files. */
    fun openFileRead(template: Template, path: String): TemplateFileReadSession {
        val file = readFile(template, path)
        return ByteArrayTemplateFileReadSession(file.size, file.modifiedAt, ByteArrayInputStream(file.content))
    }

    /**
     * Starts an atomic streamed write. Providers should stage the data and publish it only from
     * commit(); S3 providers should use multipart upload and abort it if the stream fails.
     * The default preserves compatibility for small-file providers but buffers the whole file.
     */
    fun beginFileWrite(
        template: Template,
        path: String,
        totalSize: Long,
        modifiedAt: Long,
    ): TemplateFileWriteSession {
        require(totalSize in 0..Int.MAX_VALUE.toLong()) {
            "Storage '${name()}' must implement streamed writes for files larger than 2 GiB"
        }
        // Avoid eagerly reserving the full file size; legacy providers still buffer, but
        // shouldn't allocate a potentially enormous array before receiving any chunks.
        val bytes = ByteArrayOutputStream()
        return object : TemplateFileWriteSession {
            private var finished = false

            override fun write(content: ByteArray, offset: Int, length: Int) {
                check(!finished) { "File write session is already finished" }
                bytes.write(content, offset, length)
            }

            override fun commit() {
                check(!finished) { "File write session is already finished" }
                check(bytes.size().toLong() == totalSize) {
                    "Received ${bytes.size()} bytes but expected $totalSize"
                }
                updateFile(template, path, bytes.toByteArray())
                finished = true
            }

            override fun abort() {
                if (!finished) {
                    finished = true
                    bytes.reset()
                }
            }

            override fun close() = abort()
        }
    }

    fun listDirectory(template: Template, path: String): List<TemplateDirectoryEntryData>
}

interface TemplateFileReadSession : AutoCloseable {
    val totalSize: Long
    val modifiedAt: Long
    fun read(buffer: ByteArray): Int
}

interface TemplateFileWriteSession : AutoCloseable {
    fun write(content: ByteArray, offset: Int = 0, length: Int = content.size)
    fun commit()
    fun abort()
}

private class ByteArrayTemplateFileReadSession(
    override val totalSize: Long,
    override val modifiedAt: Long,
    private val input: InputStream,
) : TemplateFileReadSession {
    override fun read(buffer: ByteArray): Int = input.read(buffer)
    override fun close() = input.close()
}
