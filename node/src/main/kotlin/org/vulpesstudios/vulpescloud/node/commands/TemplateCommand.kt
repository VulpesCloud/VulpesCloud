/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.commands

import build.buf.gen.vulpescloud.templates.v1.listRegisteredStoragesRequest
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Command
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.command.CommandSource
import org.vulpesstudios.vulpescloud.node.templates.TemplateRegistry

class TemplateCommand {

    @Command("template list")
    fun listTemplates(source: CommandSource) {
        runBlocking {
            val templates = TemplateRegistry.getAll()
            source.sendMessage("The following ${templates.size} template(s) are registered:")
            templates.forEach { template ->
                val additional = if (!template.location.nodeName.isNullOrBlank()) "-${template.location.nodeName}" else ""
                source.sendMessage(" - ${template.name}@${template.location.storageName}$additional | Weight: ${template.weight} | Enabled: ${template.enabled}")
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
                val node = if (storage.nodeName != null) "@${storage.nodeName}" else ""
                source.sendMessage(" - Type: ${storage.type.name} | Name:${storage.name}$node")
            }
        }
    }


}
