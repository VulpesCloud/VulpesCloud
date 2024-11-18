package de.vulpescloud.node.setup.answer

interface SetupAnswer {

    fun suggest(): Collection<String>

}