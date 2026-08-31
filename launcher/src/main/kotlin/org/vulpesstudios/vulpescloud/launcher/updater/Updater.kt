/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.launcher.updater

import org.vulpesstudios.vulpescloud.launcher.VulpesLauncher.Companion.GITHUBURL
import org.vulpesstudios.vulpescloud.launcher.VulpesLauncher.Companion.config
import org.vulpesstudios.vulpescloud.launcher.util.ChecksumUtil
import org.vulpesstudios.vulpescloud.launcher.util.FileUpdaterUtil
import java.io.File
import java.net.URI

object Updater {
    /**
     * Updates the specified VulpesCloud dependency jar file by downloading it from the configured Jenkins job.
     *
     * @param jarName The name of the jar file to update (e.g., "vulpescloud-api.jar").
     */
    fun updateDependency(jarName: String) {
        val checksum = ChecksumUtil.returnChecksumJson().getString(jarName)

        val depsFolder = File("launcher/dependencies/vulpescloud")
        if (!depsFolder.exists()) {
            depsFolder.mkdirs()
        }

        val target = File("launcher/dependencies/vulpescloud/$jarName.jar")
        val branchName = config.autoUpdatesBranch()

        val downloadUri = URI("${GITHUBURL}refs/heads/$branchName/$jarName.jar")

        FileUpdaterUtil.updateFile(target, downloadUri, checksum)
    }
}