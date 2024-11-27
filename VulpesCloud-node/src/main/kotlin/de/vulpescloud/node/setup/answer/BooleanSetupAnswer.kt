package de.vulpescloud.node.setup.answer

class BooleanSetupAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        return listOf("true", "false")
    }
}