package de.vulpescloud.node.setup.answers

interface SetupAnswer {

    fun suggest(): Collection<String>

}
