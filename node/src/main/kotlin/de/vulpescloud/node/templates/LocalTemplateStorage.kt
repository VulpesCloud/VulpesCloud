package de.vulpescloud.node.templates

import build.buf.gen.vulpescloud.templates.v1.TemplateStorageType
import de.vulpescloud.api.templates.Template
import de.vulpescloud.node.Node
import de.vulpescloud.node.utils.FileUtils
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

class LocalTemplateStorage : TemplateStorage {
    override fun name(): String = "LOCAL"
    override fun type(): TemplateStorageType = TemplateStorageType.newBuilder().setName("LOCAL").build()
    override fun nodeName(): String = Node.instance.configProvider.config.nodeName

    private val templatesPath = Path("local/templates")

    init {
        templatesPath.toFile().mkdirs()
    }

    private fun rootOf(template: Template): Path = templatesPath.resolve(template.name)

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

    override fun templates(): List<Template> {
        TODO("Unimplemented Method!")
//        return templatesPath
//            .toFile()
//            .listFiles()
//            ?.filter { it.isDirectory }
//            ?.map { Template(it.name, -1) } ?: emptyList()
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
        require(Files.exists(target) && !Files.isDirectory(target)) { "File '$path' does not exist" }

        return TemplateFileData(
            path = path,
            content = Files.readAllBytes(target),
            size = Files.size(target),
            mimeType = FileUtils.guessMimeType(target),
            modifiedAt = Files.getLastModifiedTime(target).toMillis(),
        )
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
            stream.map { entry ->
                TemplateDirectoryEntryData(
                    name = entry.fileName.toString(),
                    path = root.relativize(entry).toString().replace('\\', '/'),
                    directory = Files.isDirectory(entry),
                    size = if (Files.isDirectory(entry)) 0 else Files.size(entry),
                    modifiedAt = Files.getLastModifiedTime(entry).toMillis(),
                )
            }.toList()
        }
    }
}
