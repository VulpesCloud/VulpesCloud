package de.vulpescloud.node.templates

import de.vulpescloud.api.templates.TemplateStorages

class TemplateStorageProvider {

    private val localTemplateStorage = LocalTemplateStorage()

    fun getTemplateStorage(storage: TemplateStorages): TemplateStorage {
        return when (storage) {
            TemplateStorages.LOCAL -> localTemplateStorage
            TemplateStorages.S3 -> LocalTemplateStorage()
        }
    }
}
