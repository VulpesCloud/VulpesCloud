package de.vulpescloud.node.setup.answer

import de.vulpescloud.api.language.Languages

class LanguageSetupAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        return listOf("en_US", "de_DE")
    }
}