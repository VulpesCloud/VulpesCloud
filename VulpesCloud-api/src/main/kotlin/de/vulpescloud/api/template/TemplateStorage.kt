package de.vulpescloud.api.template

import de.vulpescloud.api.Named
import java.nio.file.Path

interface TemplateStorage : Named {

    fun copyTemplateToPath(template: Template, path: Path)

    fun copyPathToTemplate(path: Path, template: Template)

    fun deleteTemplate(template: Template)

    fun createTemplate(template: Template)

    fun hasTemplate(template: Template): Boolean

    fun templates(): List<Template>

}
