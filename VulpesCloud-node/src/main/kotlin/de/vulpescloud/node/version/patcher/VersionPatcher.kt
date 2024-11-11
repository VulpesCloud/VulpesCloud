package de.vulpescloud.node.version.patcher

import de.vulpescloud.node.service.LocalService
import java.io.File

interface VersionPatcher {
    fun patch(serverFile: File, localService: LocalService)

    fun id(): String?
}