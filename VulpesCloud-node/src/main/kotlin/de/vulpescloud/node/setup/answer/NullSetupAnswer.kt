package de.vulpescloud.node.setup.answer

class NullSetupAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        return emptyList()
    }
}