/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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