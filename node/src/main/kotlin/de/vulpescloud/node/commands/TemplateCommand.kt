package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.templates.v1.listRegisteredStoragesRequest
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.templates.TemplateRegistry
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Command

class TemplateCommand {

    @Command("template list")
    fun listTemplates(source: CommandSource) {
        runBlocking {
            val templates = TemplateRegistry.getAll()
            source.sendMessage("The following ${templates.size} template(s) are registered:")
            templates.forEach { template ->
                val additional = if (template.location.nodeName != null) "-${template.location.nodeName}" else ""
                source.sendMessage(" - ${template.name}@${template.location.storageName}$additional | ${template.weight} | ${template.enabled}")
            }
        }
    }

    @Command("template storages")
    fun listStorages(source: CommandSource) {
        runBlocking {
            val storages = Node.instance.localGrpcClient.templateAPI.listRegisteredStorages(
                listRegisteredStoragesRequest {  }).storageList
            source.sendMessage("The following ${storages.size} storages(s) are registered:")
            storages.forEach { storage ->
                val node = if (storage.nodeName != null) "${storage.nodeName}" else ""
                source.sendMessage(" - ${storage.type} | ${storage.name}$node")
            }
        }
    }


}
