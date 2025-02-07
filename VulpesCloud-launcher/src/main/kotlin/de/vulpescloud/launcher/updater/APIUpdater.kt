package de.vulpescloud.launcher.updater

import de.vulpescloud.launcher.VulpesLauncher.Companion.config
import de.vulpescloud.launcher.VulpesLauncher.Companion.githubURL
import de.vulpescloud.launcher.util.ChecksumUtil
import de.vulpescloud.launcher.util.FileUpdaterUtil
import java.io.File
import java.net.URI

class APIUpdater {

    fun updateAPI() {
        val wantedChecksum = ChecksumUtil.returnChecksumJson().getString("api")
        val target = File("launcher/dependencies/vulpescloud-api.jar")
        val downloadUri = URI(githubURL + config.autoUpdatesBranch() + "/vulpescloud-api.jar")

        FileUpdaterUtil.updateFile(target, downloadUri, wantedChecksum)
    }

}