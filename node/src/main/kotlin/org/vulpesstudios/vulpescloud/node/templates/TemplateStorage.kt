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

    fun listDirectory(template: Template, path: String): List<TemplateDirectoryEntryData>
}
