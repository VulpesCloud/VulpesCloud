package de.vulpescloud.node.setup.answers

class BooleanSetupAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        return listOf("true", "false", "no", "yes", "n", "y")
    }
}