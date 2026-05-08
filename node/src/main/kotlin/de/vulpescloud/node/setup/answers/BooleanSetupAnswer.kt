package de.vulpescloud.node.setup.answers

class BooleanSetupAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        return listOf("true", "false", "no", "yes", "n", "y")
    }

    companion object {
        fun parseBoolean(value: String): Boolean {
            return when (value.lowercase()) {
                "true", "yes", "y" -> true
                "false", "no", "n" -> false
                else -> throw IllegalArgumentException("Invalid boolean value: $value")
            }
        }
    }
}