package de.vulpescloud.launcher.updater

import de.vulpescloud.launcher.VulpesLauncher.Companion.config
import de.vulpescloud.launcher.VulpesLauncher.Companion.githubURL
import de.vulpescloud.launcher.util.ChecksumUtil
import de.vulpescloud.launcher.util.FileUpdaterUtil
import java.io.File
import java.net.URI

class ConnectorUpdater {

    fun updateConnector() {
        val wantedChecksum = ChecksumUtil.returnChecksumJson().getString("connector")
        val target = File("launcher/dependencies/vulpescloud-connector.jar")
        val downloadUri =
            if (config.autoUpdatesBranch() == "jenkins") {
                URI(
                    "https://jenkins.vulpescloud.de/job/VulpesCloud/lastSuccessfulBuild/artifact/VulpesCloud-connector/build/libs/vulpescloud-connector.jar"
                )
            } else {
                URI(githubURL + config.autoUpdatesBranch() + "/vulpescloud-connector.jar")
            }

        FileUpdaterUtil.updateFile(target, downloadUri, wantedChecksum)
    }

}