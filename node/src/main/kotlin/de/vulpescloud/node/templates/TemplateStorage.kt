package de.vulpescloud.node.templates

import de.vulpescloud.api.templates.Template
import java.nio.file.Path

interface TemplateStorage {

    fun name(): String

    fun copyTemplateToPath(template: Template, path: Path)

    fun copyPathToTemplate(path: Path, template: Template)

    fun deleteTemplate(template: Template)

    fun createTemplate(template: Template)

    fun hasTemplate(template: Template): Boolean

    fun templates(): List<Template>

    // TODO: We need some more methods that will allow us to move a template from node to node or node to S3 and so on

}
