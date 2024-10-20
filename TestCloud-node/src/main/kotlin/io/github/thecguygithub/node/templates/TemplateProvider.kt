package io.github.thecguygithub.node.templates

import lombok.SneakyThrows
import java.nio.file.Files
import java.nio.file.Path


class TemplatesProvider {
    private val templates: MutableList<Template> = ArrayList()

    @SneakyThrows
    fun prepareTemplate(templateId: String?) {
        val templatePath = TEMPLATE_DIR.resolve(templateId!!)
        templates.add(Template(templateId))

        if (!Files.exists(templatePath)) {
            Files.createDirectory(templatePath)
        }
    }

    fun prepareTemplate(templateIds: Array<String?>) {
        for (templateId in templateIds) {
            this.prepareTemplate(templateId)
        }
    }

    companion object {
        private val TEMPLATE_DIR: Path = Path.of("templates")

        init {
            TEMPLATE_DIR.toFile().mkdirs()
        }
    }
}