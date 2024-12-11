package de.vulpescloud.node.version.patcher

object VersionPatcherPool {

    val patcherList: MutableMap<String?, VersionPatcher> = HashMap()

    init {
        register(PaperPatcher());
    }

    private fun register(patcher: VersionPatcher) {
        patcherList[patcher.id()] = patcher
    }

    fun patcher(id: String?): VersionPatcher? {
        return patcherList[id]
    }

}