package de.vulpescloud.launcher.updater

import de.vulpescloud.launcher.VulpesLauncher.Companion.config
import de.vulpescloud.launcher.VulpesLauncher.Companion.githubURL
import de.vulpescloud.launcher.util.ChecksumUtil
import de.vulpescloud.launcher.util.FileUpdaterUtil
import java.io.File
import java.net.URI

class WrapperUpdater {

    fun updateWrapper() {
        val wantedChecksum = ChecksumUtil.returnChecksumJson().getString("wrapper")
        val target = File("launcher/dependencies/vulpescloud-wrapper.jar")
        val downloadUri =
            if (config.autoUpdatesBranch() == "jenkins") {
                URI(
                    "https://jenkins.vulpescloud.de/job/VulpesCloud/lastSuccessfulBuild/artifact/build/meta-repo/vulpescloud-wrapper.jar"
                )
            } else {
                URI(githubURL + config.autoUpdatesBranch() + "/vulpescloud-wrapper.jar")
            }

        FileUpdaterUtil.updateFile(target, downloadUri, wantedChecksum)
    }

}