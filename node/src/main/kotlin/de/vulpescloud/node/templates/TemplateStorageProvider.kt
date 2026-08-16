package de.vulpescloud.node.templates

import de.vulpescloud.api.templates.TemplateStorages
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves [TemplateStorage] implementations by their [TemplateStorages] type.
 *
 * [TemplateStorages.LOCAL] is always available. Other storages (e.g. an S3-backed storage) are
 * not implemented in this project - they are expected to be provided by an external module,
 * which should call [registerStorage] (e.g. from [de.vulpescloud.node.modules.VulpesModule.onEnable])
 * to make itself available.
 */
class TemplateStorageProvider {

    private val localTemplateStorage = LocalTemplateStorage()
    private val storages = ConcurrentHashMap<TemplateStorages, TemplateStorage>()

    init {
        storages[TemplateStorages.LOCAL] = localTemplateStorage
    }

    fun registerStorage(storage: TemplateStorages, implementation: TemplateStorage) {
        storages[storage] = implementation
    }

    fun unregisterStorage(storage: TemplateStorages) {
        if (storage == TemplateStorages.LOCAL) return
        storages.remove(storage)
    }

    fun isRegistered(storage: TemplateStorages): Boolean = storages.containsKey(storage)

    fun getTemplateStorage(storage: TemplateStorages): TemplateStorage {
        return storages[storage]
            ?: throw IllegalStateException(
                "No TemplateStorage registered for $storage. Is the corresponding module loaded?"
            )
    }
}
