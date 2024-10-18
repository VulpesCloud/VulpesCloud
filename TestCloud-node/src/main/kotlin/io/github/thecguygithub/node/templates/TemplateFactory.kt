package io.github.thecguygithub.node.templates

import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.service.ClusterLocalServiceImpl
import io.github.thecguygithub.node.util.DirectoryActions
import java.nio.file.Path


object TemplateFactory {

    val logger = Logger()

    fun cloneTemplate(localService: ClusterLocalServiceImpl) {
        for (template in localService.group().templates()!!) {
            val templatePath = Path.of("templates/$template")

            logger.debug("Copy template $template to ${localService.name()}")
            DirectoryActions.copyDirectoryContents(templatePath, localService.runningDir)
        }
    }

    fun copyService(localService: ClusterLocalServiceImpl, template: Template) {
        val templatePath = Path.of("templates/" + template.templateId)
        val servicePath = localService.runningDir

        logger.debug("Copy service ${localService.name()} to $templatePath")
        DirectoryActions.copyDirectoryContents(servicePath, templatePath)
    }

}