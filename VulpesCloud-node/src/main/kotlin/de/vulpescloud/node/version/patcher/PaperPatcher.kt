/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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