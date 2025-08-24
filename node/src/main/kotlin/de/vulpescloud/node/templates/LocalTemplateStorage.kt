package de.vulpescloud.node.templates

import de.vulpescloud.api.templates.Template
import de.vulpescloud.api.templates.TemplateStorages
import de.vulpescloud.node.utils.FileUtils
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

class LocalTemplateStorage : TemplateStorage {
    override fun name(): String = "LOCAL"

    private val templatesPath = Path("local/templates")

    init {
        templatesPath.toFile().mkdirs()
    }

    override fun copyTemplateToPath(template: Template, path: Path) {
        val templatePath = templatesPath.resolve(template.name)
        templatePath.toFile().mkdirs()
        FileUtils.copyDir(templatePath, path)
    }

    override fun copyPathToTemplate(path: Path, template: Template) {
        val templatePath = templatesPath.resolve(template.name)
        templatePath.toFile().mkdirs()
        FileUtils.copyDir(path, templatePath)
    }

    override fun deleteTemplate(template: Template) {
        FileUtils.deleteDir(templatesPath.resolve(template.name))
    }

    override fun createTemplate(template: Template) {
        templatesPath.resolve(template.name).toFile().mkdirs()
    }

    override fun hasTemplate(template: Template): Boolean {
        return Files.exists(templatesPath.resolve(template.name))
    }

    override fun templates(): List<Template> {
        return templatesPath
            .toFile()
            .listFiles()
            ?.filter { it.isDirectory }
            ?.map { Template(it.name, TemplateStorages.LOCAL, 0) } ?: emptyList()
    }
}
