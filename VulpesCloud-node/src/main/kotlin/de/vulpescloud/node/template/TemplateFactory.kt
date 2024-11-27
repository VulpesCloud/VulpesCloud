package de.vulpescloud.node.template

import de.vulpescloud.node.service.LocalService
import de.vulpescloud.node.util.DirectoryActions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

object TemplateFactory {

    val logger: Logger = LoggerFactory.getLogger(TemplateFactory::class.java)

    fun cloneTemplate(localService: LocalService) {
        for (template in localService.task.templates()) {
            val templatePath = Path.of("templates/$template")

            logger.debug("Copy template $template to ${localService.name()}")
            DirectoryActions.copyDirectoryContents(templatePath, localService.runningDir)
        }
    }

    fun copyService(localService: LocalService, template: Template) {
        val templatePath = Path.of("templates/" + template.templateId)
        val servicePath = localService.runningDir

        logger.debug("Copy service {} to {}", localService.name(), templatePath)
        DirectoryActions.copyDirectoryContents(servicePath, templatePath)
    }

}