package de.vulpescloud.node.version.patcher

import de.vulpescloud.node.service.LocalService
import org.jetbrains.annotations.Contract
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class PaperPatcher : VersionPatcher {

    override fun patch(serverFile: File, localService: LocalService) {
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

    @Contract(pure = true)
    override fun id(): String {
        return "paperclip"
    }
}