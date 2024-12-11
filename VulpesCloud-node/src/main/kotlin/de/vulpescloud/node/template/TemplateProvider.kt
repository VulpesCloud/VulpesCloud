package de.vulpescloud.node.template

import java.nio.file.Files
import java.nio.file.Path

class TemplateProvider {
    private val templates: MutableList<Template> = ArrayList()

    init {
        TEMPLATE_DIR.toFile().mkdirs()
    }

    fun prepareTemplate(templateId: String?) {
        val templatePath = TEMPLATE_DIR.resolve(templateId!!)
        templates.add(Template(templateId))

        if (!Files.exists(templatePath)) {
            Files.createDirectory(templatePath)
        }
    }

    fun prepareTemplate(templateIds: List<String?>) {
        for (templateId in templateIds) {
            this.prepareTemplate(templateId)
        }
    }

    companion object {
        private val TEMPLATE_DIR: Path = Path.of("local/templates")
    }
}