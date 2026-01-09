package de.vulpescloud.node.setup.answers

import de.vulpescloud.node.serversoftware.impl.*
import de.vulpescloud.node.setup.setups.TaskSetup
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class SoftwareVersionSetupAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        return CompletableFuture.supplyAsync {
                runBlocking {
                    val softwareName = TaskSetup.softwareName ?: return@runBlocking emptyList()
                    val versions =
                        when (softwareName) {
                            "Velocity" -> VelocityDownloader.getAvailableVersions()
                            "Paper" -> PaperDownloader.getAvailableVersions()
                            "Purpur" -> PurpurDownloader.getAvailableVersions()
                            "Folia" -> FoliaDownloader.getAvailableVersions()
                            "Canvas" -> CanvasDownloader.getAvailableVersions()
                            else -> emptyList()
                        }

                    versions.map { it.version }
                }
            }
            .get(5, TimeUnit.SECONDS)
    }
}
