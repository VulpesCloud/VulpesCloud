package de.vulpescloud.node.setup.answers

class SoftwareNameSetupAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        return listOf("Purpur", "Paper", "Velocity", "Canvas", "Folia", "Minestom")
    }
}
