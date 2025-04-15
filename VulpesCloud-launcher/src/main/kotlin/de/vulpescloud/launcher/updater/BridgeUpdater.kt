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
        val downloadUri =
            if (config.autoUpdatesBranch() == "jenkins") {
                URI(
                    "https://jenkins.vulpescloud.de/job/VulpesCloud/lastSuccessfulBuild/artifact/VulpesCloud-bridge/build/libs/vulpescloud-bridge.jar"
                )
            } else {
                URI(githubURL + config.autoUpdatesBranch() + "/vulpescloud-bridge.jar")
            }

        FileUpdaterUtil.updateFile(target, downloadUri, wantedChecksum)
    }

}