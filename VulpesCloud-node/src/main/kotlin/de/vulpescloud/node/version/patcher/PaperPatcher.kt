package de.vulpescloud.node.version.patcher

import de.vulpescloud.node.services.LocalServiceImpl
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class PaperPatcher : VersionPatcher {
    override fun patch(serverFile: File, service: LocalServiceImpl) {
        val process = ProcessBuilder(
            "java",
            "-Dpaperclip.patchonly=true",
            "-jar",
            serverFile.name
        ).directory(serverFile.parentFile).start()
        val inputStreamReader: InputStreamReader = InputStreamReader(process.inputStream)
        val bufferedReader = BufferedReader(inputStreamReader)

        process.waitFor()
        process.destroyForcibly()
        bufferedReader.close()
        inputStreamReader.close()
    }

    override fun id(): String {
        return "paperclip"
    }
}