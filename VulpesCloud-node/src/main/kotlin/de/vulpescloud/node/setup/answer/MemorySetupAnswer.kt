package de.vulpescloud.node.setup.answer

class MemorySetupAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        return listOf("512", "1024", "2048", "4096")
    }
}