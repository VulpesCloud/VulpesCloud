package de.vulpescloud.node.setup.setups

import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.annotations.SetupCancel
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion

class DemoSetup : Setup {

    @SetupQuestion(0, "What is the hostname of the node?")
    fun hostname(hostname: String): Boolean {
        println("The hostname is: $hostname")
        return true
    }

    @SetupQuestion(1, "What is the port of the node?")
    fun port(port: Int): Boolean {
        println("The port is: $port")
        return true
    }

    @SetupQuestion(2, "What is the password of the node?")
    fun password(password: String): Boolean {
        println("The password is: $password")

        // logic to jump to question index 5

        return true
    }

    // Will be skipped
    @SetupQuestion(3, "What is the path to the node jar?")
    fun path(path: String): Boolean {
        println("The path is: $path")
        return true
    }

    @SetupQuestion(5, "Do you want to enable the debug mode?")
    fun debug(debug: Boolean): Boolean {
        println("The debug mode is: $debug")
        return true
    }

    @SetupFinish
    fun finish() {
        println("The setup is finished!")
    }

    @SetupCancel
    fun cancel() {
        println("The setup was cancelled!")
    }

}
