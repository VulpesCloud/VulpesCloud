package de.vulpescloud.node.template

import de.vulpescloud.api.service.Service
import de.vulpescloud.api.template.Template
import de.vulpescloud.api.template.TemplateStorage
import de.vulpescloud.node.utils.FileUtils
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

class LocalTemplateStorage : TemplateStorage {

    private val templatesPath = Path("local/templates")

    init {
        templatesPath.toFile().mkdirs()
    }

    override fun copyTemplateToService(template: Template, service: Service) {
        FileUtils.copyDir(templatesPath.resolve(template.name), service.path())
    }

    override fun copyServiceToTemplate(service: Service, template: Template) {
        FileUtils.copyDir(service.path(), templatesPath.resolve(template.name))
    }

    override fun copyTemplateToPath(template: Template, path: Path) {
        FileUtils.copyDir(templatesPath.resolve(template.name), path)
    }

    override fun copyTemplateToTemplate(template: Template, target: Template) {
        FileUtils.copyDir(
            templatesPath.resolve(template.name),
            templatesPath.resolve(templatesPath.resolve(template.name)),
        )
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
            ?.map { Template(it.name, "local") } ?: emptyList()
    }

    override fun name(): String {
        return "local"
    }
}
