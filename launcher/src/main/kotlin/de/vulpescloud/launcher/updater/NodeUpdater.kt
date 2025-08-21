package de.vulpescloud.launcher.updater

import de.vulpescloud.launcher.VulpesLauncher.Companion.config
import de.vulpescloud.launcher.VulpesLauncher.Companion.githubURL
import de.vulpescloud.launcher.util.ChecksumUtil
import de.vulpescloud.launcher.util.FileUpdaterUtil
import java.io.File
import java.net.URI

class NodeUpdater {

    fun updateNode() {
        val wantedChecksum = ChecksumUtil.returnChecksumJson().getString("node")
        val target = File("launcher/dependencies/vulpescloud-node.jar")
        val downloadUri =
            if (config.autoUpdatesBranch() == "jenkins") {
                URI(
                    "https://jenkins.vulpescloud.de/job/VulpesCloud/lastSuccessfulBuild/artifact/build/meta-repo/vulpescloud-node.jar"
                )
            } else {
                URI(githubURL + config.autoUpdatesBranch() + "/vulpescloud-node.jar")
            }

        FileUpdaterUtil.updateFile(target, downloadUri, wantedChecksum)
    }

}