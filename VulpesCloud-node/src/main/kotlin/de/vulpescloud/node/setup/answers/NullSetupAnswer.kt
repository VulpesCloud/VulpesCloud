package de.vulpescloud.node.setup.answers

class NullSetupAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        return emptyList()
    }
}