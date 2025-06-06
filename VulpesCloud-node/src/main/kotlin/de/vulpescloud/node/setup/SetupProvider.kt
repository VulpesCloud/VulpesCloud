package de.vulpescloud.node.setup

interface SetupProvider {
    var currentSetup: SetupInfo?

    fun startSetup(setup: Setup)

    fun cancelSetup()

    fun input(input: String)

    fun getSetupAnswers(input: String): List<String>

}
