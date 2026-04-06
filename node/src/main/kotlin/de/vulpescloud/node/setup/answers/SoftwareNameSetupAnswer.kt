package de.vulpescloud.node.setup.answers

import de.vulpescloud.node.Node

class SoftwareNameSetupAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        return Node.instance.serverSoftwareProvider
            .downloaders()
            .map { it.displayName }
            .toMutableList()
            .apply { add("Minestom") }
    }
}
