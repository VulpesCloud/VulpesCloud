package de.vulpescloud.node.setup.setups

import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.annotations.SetupCancel
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import kotlin.properties.Delegates

class FirstSetup : Setup {

    private lateinit var language: String
    private lateinit var hostname: String
    private var grpcPort by Delegates.notNull<Int>()
    private lateinit var grpcHostname: String
    private lateinit var name: String

    @SetupQuestion(0, "What language do you want to use?")
    fun name(name: String): Boolean {
        println("The name is: $name")
        return true
    }

    @SetupQuestion(1, "What is the hostname of the node?")
    fun hostname(hostname: String): Boolean {
        println("The hostname is: $hostname")
        return true
    }

    @SetupQuestion(2, "What is the port of the node?")
    fun port(port: Int): Boolean {
        println("The port is: $port")
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
