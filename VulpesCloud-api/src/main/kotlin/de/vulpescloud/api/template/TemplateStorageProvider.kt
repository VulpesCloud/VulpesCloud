package de.vulpescloud.api.template

interface TemplateStorageProvider {

    fun getTemplateStorageByName(name: String): TemplateStorage?

    fun getTemplateStorages(): List<TemplateStorage>

    fun registerTemplateStorage(templateStorage: TemplateStorage)

}
