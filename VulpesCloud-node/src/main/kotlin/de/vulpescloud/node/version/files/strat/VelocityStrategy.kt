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

package de.vulpescloud.node.version.files.strat

import de.vulpescloud.node.version.files.VersionFile
import de.vulpescloud.node.version.files.VersionFileReplacement
import de.vulpescloud.node.version.files.VersionFileStrategy

object VelocityStrategy {

    val files: List<VersionFile> = listOf(
        VersionFile(
            "eula.txt",
            VersionFileStrategy.DIRECT_CREATE,
            listOf(),
            listOf("eula=true")
        ),
        VersionFile(
            "server.properties",
            VersionFileStrategy.COPY_FROM_CLASSPATH_IF_NOT_EXISTS,
            listOf(
                VersionFileReplacement(
                    "server-port",
                    "%port%"
                ),
                VersionFileReplacement(
                    "online-mode",
                    "false"
                )
            ),
            listOf()
        )
    )

}