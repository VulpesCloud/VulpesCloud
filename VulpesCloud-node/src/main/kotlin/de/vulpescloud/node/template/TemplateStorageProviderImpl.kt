package de.vulpescloud.node.template

import de.vulpescloud.api.template.TemplateStorage
import de.vulpescloud.api.template.TemplateStorageProvider
import org.slf4j.LoggerFactory

class TemplateStorageProviderImpl : TemplateStorageProvider {
    private val logger = LoggerFactory.getLogger(TemplateStorageProviderImpl::class.java)
    private val templateStorages = mutableListOf<TemplateStorage>()

    override fun getTemplateStorageByName(name: String): TemplateStorage? {
        return templateStorages.find { it.name() == name }
    }

    override fun getTemplateStorages(): List<TemplateStorage> {
        return templateStorages
    }

    override fun registerTemplateStorage(templateStorage: TemplateStorage) {
        logger.debug("Registering template storage: ${templateStorage.name()}")
        templateStorages.add(templateStorage)
    }
}
