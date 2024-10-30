package io.github.thecguygithub.node.version.files.strat

import io.github.thecguygithub.node.version.files.VersionFile
import io.github.thecguygithub.node.version.files.VersionFileReplacement
import io.github.thecguygithub.node.version.files.VersionFileStrategy

object PaperStrategy {

    val files: List<VersionFile> = listOf(
        VersionFile(
            "eula.txt",
            VersionFileStrategy.DIRECT_CREATE,
            listOf(),
            listOf("eula=true")
        ),
//        VersionFile(
//            "server.properties",
//            VersionFileStrategy.COPY_FROM_CLASSPATH_IF_NOT_EXISTS,
//            listOf(
//                VersionFileReplacement(
//                    "server-port",
//                    "%port%"
//                ),
//                VersionFileReplacement(
//                    "online-mode",
//                    "false"
//                )
//            ),
//            listOf()
//        )
    )

}