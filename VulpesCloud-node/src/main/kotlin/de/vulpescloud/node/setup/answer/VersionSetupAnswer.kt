package de.vulpescloud.node.setup.answer

import de.vulpescloud.node.Node

class VersionSetupAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        val verNames: MutableList<String> = mutableListOf()
            Node.versionProvider.versions.forEach { verNames.add(it.name) }
        return verNames.toList()
    }
}