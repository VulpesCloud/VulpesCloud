package io.github.thecguygithub.node.platforms.patcher


object PlatformPatcherPool {

    val patcherList: MutableMap<String?, PlatformPatcher> = HashMap()

    init {
        register(PlatformPaperPatcher());
    }

    fun register(patcher: PlatformPatcher) {
        patcherList[patcher.id()] = patcher
    }

    fun patcher(id: String?): PlatformPatcher? {
        return patcherList[id]
    }

}