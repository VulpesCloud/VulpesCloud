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