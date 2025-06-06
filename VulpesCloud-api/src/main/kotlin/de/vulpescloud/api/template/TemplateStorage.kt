package de.vulpescloud.api.template

import de.vulpescloud.api.Named
import de.vulpescloud.api.service.Service
import java.nio.file.Path

interface TemplateStorage : Named {

    fun copyTemplateToService(template: Template, service: Service)

    fun copyTemplateToPath(template: Template, path: Path)

    fun copyServiceToTemplate(service: Service, template: Template)

    fun copyTemplateToTemplate(template: Template, target: Template)

    fun deleteTemplate(template: Template)

    fun createTemplate(template: Template)

    fun hasTemplate(template: Template): Boolean

    fun templates(): List<Template>

}
