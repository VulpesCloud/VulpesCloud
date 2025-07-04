package de.vulpescloud.node.setup.answers

class LanguageSetupAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        return listOf("en_US", "de_DE")
    }
}