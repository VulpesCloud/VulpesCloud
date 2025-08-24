package de.vulpescloud.node.setup.setups

import de.vulpescloud.node.Node
import de.vulpescloud.node.config.MongoConfig
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import de.vulpescloud.node.setup.answers.MemorySetupAnswer
import de.vulpescloud.node.setup.answers.SetupAnswer
import java.util.UUID

class FirstSetup : Setup {
    override val header = "test setup"

    private var eulaAccepted = false
    private var grpcAddress = "0.0.0.0:6565"
    private var nodeName = "Node-1"

    private val systemMemoryInMb: Long = Runtime.getRuntime().maxMemory() / 1024 / 1024
    private var totalAllowedMemoryMb: Long = systemMemoryInMb

    private var mongoConnectionString: String = "mongodb://localhost:27017/"
    private var mongoDatabase: String = "vulpescloud"
    private var mongoCollectionPrefix: String = "vc_"

    private val serviceType: String = "LOCAL" // can be "LOCAL" or "DOCKER"

    class EulaAnswer : SetupAnswer {
        override fun suggest(): Collection<String> {
            return listOf("yes")
        }
    }

    class GrpcAddressAnswer : SetupAnswer {
        override fun suggest(): Collection<String> {
            return listOf("127.0.0.1:6565", "0.0.0.0:6565")
        }
    }

    class ServiceTypeAnswer : SetupAnswer {
        override fun suggest(): Collection<String> {
            return listOf("LOCAL", "DOCKER")
        }
    }

    @SetupQuestion(index = 0, translationKey = "Do you agree to the Mojang EULA (https://aka.ms/MinecraftEULA)?", EulaAnswer::class, true, ["yes"])
    fun q1(answer: String) : Boolean {
        if (answer.lowercase() == "yes") {
            eulaAccepted = true
            return true
        } else {
            eulaAccepted = false
            return false
        }
    }

    @SetupQuestion(index = 1, translationKey = "On which host and port should we start the gRPC Server? (Used for communicating between nodes and the services)", forceAnswer = false, default = ["127.0.0.1:6565"], answer = GrpcAddressAnswer::class)
    fun q2(answer: String) : Boolean {

        val matchesFormat = Regex("^([0-9]{1,3}\\.){3}[0-9]{1,3}:[0-9]{1,5}\$").matches(answer)
        if (!matchesFormat) {
            return false
        }

        val parts = answer.split(":")
        val ipParts = parts[0].split(".").map { it.toInt() }
        val port = parts[1].toInt()
        if (ipParts.any { it !in 0..255 } || port < 1 || port > 65535) {
            return false
        }
        grpcAddress = answer
        return true
    }

    @SetupQuestion(
        index = 2,
        translationKey = "How much memory should all services be able to use? (Value must be in MB)",
        forceAnswer = false,
        answer = MemorySetupAnswer::class
    )
    fun q3(answer: String): Boolean {
        val effective = (answer.ifBlank { systemMemoryInMb.toString() }).trim()
        val value = effective.toLongOrNull() ?: return false
        if (value <= 0) return false
        if (value > systemMemoryInMb) return false
        totalAllowedMemoryMb = value
        return true
    }

    @SetupQuestion(
        index = 3,
        translationKey = "What name should this node have?",
        forceAnswer = false,
        default = ["Node-1"]
    )
    fun q4(answer: String): Boolean {
        val effective = answer.ifBlank { "Node-1" }.trim()
        if (effective.length !in 3..32) return false
        nodeName = effective
        return true
    }

    @SetupQuestion(
        index = 4,
        translationKey = "Which service type should be used? (LOCAL or DOCKER)",
        forceAnswer = true,
        default = ["LOCAL"],
        answer = ServiceTypeAnswer::class
    )
    fun q5(answer: String): Boolean {
        val effective = answer.ifBlank { "LOCAL" }.trim().uppercase()
        return !(effective != "LOCAL" && effective != "DOCKER")
    }

    @SetupQuestion(
        index = 5,
        translationKey = "Please enter the MongoDB connection string",
        forceAnswer = false,
        default = ["mongodb://localhost:27017/"]
    )
    fun q6(answer: String): Boolean {
        val effective = answer.ifBlank { mongoConnectionString }.trim()
        if (!(!effective.startsWith("mongodb://") && !effective.startsWith("mongodb+srv://"))) {
            mongoConnectionString = effective
            return true
        } else {
            return false
        }
    }

    @SetupQuestion(
        index = 6,
        translationKey = "Please enter the MongoDB database name",
        forceAnswer = false,
        default = ["vulpescloud"]
    )
    fun q7(answer: String): Boolean {
        val effective = answer.ifBlank { mongoDatabase }.trim()
        if (effective.isNotEmpty()) {
            mongoDatabase = effective
            return true
        } else {
            return false
        }
    }

    @SetupQuestion(
        index = 7,
        translationKey = "Please enter the MongoDB collection prefix (keep default if you don't know what this is)",
        forceAnswer = false,
        default = ["vc_"]
    )
    fun q8(answer: String): Boolean {
        val effective = answer.ifBlank { mongoCollectionPrefix }.trim()
        if (effective.isNotEmpty()) {
            mongoCollectionPrefix = effective
            return true
        } else {
            return false
        }
    }

    @SetupFinish
    fun finish() {
        Node.instance.configProvider.updateConfig(
            NodeConfig(
                nodeName,
                UUID.randomUUID(),
                grpcAddress.split(":")[1].toInt(),
                grpcAddress.split(":")[0],
                MongoConfig(
                    mongoConnectionString,
                    mongoDatabase,
                    mongoCollectionPrefix
                ),
                totalAllowedMemoryMb.toInt(),
                serviceType
            )
        )

        Node.instance.terminal.changePrompt("")
    }
}