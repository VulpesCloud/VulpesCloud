package de.vulpescloud.node.setup.setups

import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.annotations.SetupQuestion
import de.vulpescloud.node.setup.answers.SetupAnswer

class FirstSetup : Setup {
    override val header = "test setup"

    private var eulaAccepted = false
    private var grpcAddress = "0.0.0.0:6565"

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
}