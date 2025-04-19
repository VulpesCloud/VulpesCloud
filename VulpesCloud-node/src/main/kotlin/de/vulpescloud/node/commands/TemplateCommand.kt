package de.vulpescloud.node.commands

import de.vulpescloud.api.template.TemplateStorageProvider
import de.vulpescloud.node.command.CommandSource
import org.incendo.cloud.annotations.Command

@Suppress("Unused")
class TemplateCommand(
    private val templateStorageProvider: TemplateStorageProvider
) {

    @Command("template|templates list")
    fun listTemplates(
        source: CommandSource
    ) {
        source.sendMessage("Listing Templates from ${templateStorageProvider.getTemplateStorages().size} Storage(s):")
        templateStorageProvider.getTemplateStorages().forEach { storage ->
            storage.templates().forEach { template ->
                source.sendMessage(" &8- &m${template.name} &8(&e${template.storage}&8)")
            }
        }
    }

}
