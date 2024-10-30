package io.github.thecguygithub.node.version.patcher

import io.github.thecguygithub.node.service.LocalService
import java.io.File

interface VersionPatcher {
    fun patch(serverFile: File, localService: LocalService)

    fun id(): String?
}