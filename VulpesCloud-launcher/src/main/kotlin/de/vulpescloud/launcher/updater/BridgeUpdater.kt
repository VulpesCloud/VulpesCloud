package de.vulpescloud.launcher.updater

import de.vulpescloud.launcher.VulpesLauncher.Companion.config
import de.vulpescloud.launcher.VulpesLauncher.Companion.githubURL
import de.vulpescloud.launcher.util.ChecksumUtil
import de.vulpescloud.launcher.util.FileUpdaterUtil
import java.io.File
import java.net.URI

class BridgeUpdater {

    fun updateBridge() {
        val wantedChecksum = ChecksumUtil.returnChecksumJson().getString("bridge")
        val target = File("launcher/dependencies/vulpescloud-bridge.jar")
        val downloadUri = URI(githubURL + config.autoUpdatesBranch() + "/vulpescloud-bridge.jar")

        FileUpdaterUtil.updateFile(target, downloadUri, wantedChecksum)
    }

}