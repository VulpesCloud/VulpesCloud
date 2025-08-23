package de.vulpescloud.launcher.updater

import de.vulpescloud.launcher.VulpesLauncher.Companion.GITHUBURL
import de.vulpescloud.launcher.VulpesLauncher.Companion.config
import de.vulpescloud.launcher.util.ChecksumUtil
import de.vulpescloud.launcher.util.FileUpdaterUtil
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