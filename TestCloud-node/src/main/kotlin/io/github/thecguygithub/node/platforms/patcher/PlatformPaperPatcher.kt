package io.github.thecguygithub.node.platforms.patcher

import io.github.thecguygithub.node.service.ClusterLocalServiceImpl
import lombok.SneakyThrows
import org.jetbrains.annotations.Contract
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


class PlatformPaperPatcher : PlatformPatcher {
    @SneakyThrows
    override fun patch(serverFile: File?, clusterLocalService: ClusterLocalServiceImpl?) {
        val process = ProcessBuilder(
            "java",
            "-Dpaperclip.patchonly=true",
            "-jar",
            serverFile?.name
        ).directory(serverFile?.parentFile).start()
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