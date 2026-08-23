package de.vulpescloud.node.templates

import build.buf.gen.vulpescloud.templates.v1.TemplateStorageType

object TemplateStorageRegistry {

    private val storages = mutableListOf<TemplateStorage>()

    fun getAllTemplateStorages(type: TemplateStorageType?): List<TemplateStorage> {
        if (type == null) return storages
        return storages.filter { it.type().name == type.name }
    }

    fun registerTemplateStorage(storage: TemplateStorage) {
        if (storages.find { it.name() == storage.name() } != null) throw IllegalArgumentException("Template storage ${storage.name()} is already registered.")
        storages.add(storage)
    }

    fun getTemplateStorageByName(name: String): TemplateStorage? {
        return storages.find { it.name() == name }
    }

    fun unregisterTemplateStorage(storage: TemplateStorage) {
        storages.remove(storage)
    }

}
