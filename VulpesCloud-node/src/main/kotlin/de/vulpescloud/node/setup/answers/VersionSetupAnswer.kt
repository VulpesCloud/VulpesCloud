package de.vulpescloud.node.setup.answers

import de.vulpescloud.api.version.VersionProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class VersionSetupAnswer : SetupAnswer, KoinComponent {
    private val versionProvider: VersionProvider by inject()

    override fun suggest(): List<String> {
        return versionProvider.getAllRegisteredVersions().map { it.name }
    }
}
