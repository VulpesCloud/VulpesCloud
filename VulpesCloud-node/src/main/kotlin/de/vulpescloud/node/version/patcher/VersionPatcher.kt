package de.vulpescloud.node.version.patcher

import de.vulpescloud.node.services.LocalServiceImpl
import java.io.File

interface VersionPatcher {
    fun patch(serverFile: File, service: LocalServiceImpl)
    // todo
    fun id(): String
}