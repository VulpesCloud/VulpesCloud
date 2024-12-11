package de.vulpescloud.node.template

import de.vulpescloud.node.services.LocalServiceImpl
import de.vulpescloud.node.util.DirectoryActions
import org.slf4j.LoggerFactory
import java.nio.file.Path

object TemplateFactory {

    private val logger = LoggerFactory.getLogger(TemplateFactory::class.java)

    fun cloneTemplate(localService: LocalServiceImpl) {
        for (template in localService.task().templates()) {
            val templatePath = Path.of("templates/$template")

            logger.debug("Copy template $template to ${localService.name()}")
            DirectoryActions.copyDirectoryContents(templatePath, localService.runningDir)
        }
    }

    fun copyService(localService: LocalServiceImpl, template: Template) {
        val templatePath = Path.of("templates/" + template.templateId)
        val servicePath = localService.runningDir

        logger.debug("Copy service {} to {}", localService.name(), templatePath)
        DirectoryActions.copyDirectoryContents(servicePath, templatePath)
    }

}