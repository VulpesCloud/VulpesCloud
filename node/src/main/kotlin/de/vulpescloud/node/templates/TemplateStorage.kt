package de.vulpescloud.node.templates

import de.vulpescloud.api.templates.Template

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

/**
 * A backend able to persist templates and their file contents.
 *
 * Implementations are looked up via [TemplateStorageProvider] by their
 * [de.vulpescloud.api.templates.TemplateStorages] value.
 * [de.vulpescloud.api.templates.TemplateStorages.LOCAL] is provided out of the box
 * ([LocalTemplateStorage]); other storages (e.g. S3) are expected to be registered at runtime by
 * an external module via [TemplateStorageProvider.registerStorage].
 *
 * Whether/how a non-local storage caches data on disk (e.g. to avoid re-downloading a template
 * from S3 for every service start) is entirely up to the implementation - callers only ever see
 * the operations below.
 */
interface TemplateStorage {

    fun name(): String

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
