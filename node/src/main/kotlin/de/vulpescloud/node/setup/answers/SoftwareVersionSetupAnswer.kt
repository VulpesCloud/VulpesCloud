package de.vulpescloud.node.setup.answers

import de.vulpescloud.node.serversoftware.impl.*
import de.vulpescloud.node.setup.setups.TaskSetup
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

class SoftwareVersionSetupAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        return CompletableFuture.supplyAsync {
                runBlocking {
                    val softwareName = TaskSetup.downloader ?: return@runBlocking emptyList()
                    val versions = softwareName.getAvailableVersions()

                    versions.map { it.version }
                }
            }
            .get(5, TimeUnit.SECONDS)
    }
}
