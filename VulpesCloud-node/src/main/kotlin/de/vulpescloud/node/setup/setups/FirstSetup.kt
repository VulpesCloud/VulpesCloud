package de.vulpescloud.node.setup.setups

import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion

class FirstSetup : Setup {

    @SetupQuestion(1, "test.test")
    fun test(test: String): Boolean {
        return true
    }

    @SetupFinish
    fun finish() {

    }

}
