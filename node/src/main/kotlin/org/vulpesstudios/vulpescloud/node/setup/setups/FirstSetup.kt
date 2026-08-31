/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.setup.setups

import kotlinx.coroutines.runBlocking
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.NodeShutdown
import org.vulpesstudios.vulpescloud.node.config.DockerConfig
import org.vulpesstudios.vulpescloud.node.config.NodeConfig
import org.vulpesstudios.vulpescloud.node.setup.Setup
import org.vulpesstudios.vulpescloud.node.setup.annotations.SetupCancel
import org.vulpesstudios.vulpescloud.node.setup.annotations.SetupFinish
import org.vulpesstudios.vulpescloud.node.setup.annotations.SetupQuestion
import org.vulpesstudios.vulpescloud.node.setup.answers.AddressAnswer
import org.vulpesstudios.vulpescloud.node.setup.answers.BooleanSetupAnswer
import org.vulpesstudios.vulpescloud.node.setup.answers.MemorySetupAnswer
import java.util.*

class FirstSetup : Setup {
    override val header = "Fist Setup"

    private var eulaAccepted = false
    private var grpcHost = "0.0.0.0"
    private var grpcPort = 6565
    private var bindAddress = "0.0.0.0"
    private var nodeName = "Node-1"

    private val systemMemoryInMb: Long = Runtime.getRuntime().maxMemory() / 1024 / 1024
    private var totalAllowedMemoryMb: Long = systemMemoryInMb
    private var modernForwardingEnabled: Boolean = false

    @SetupQuestion(
        index = 0,
        translationKey = "Do you agree to the Mojang EULA (https://aka.ms/MinecraftEULA)?",
        BooleanSetupAnswer::class,
        true,
    )
    fun q1(answer: String): Boolean {
        eulaAccepted = BooleanSetupAnswer.parseBoolean(answer)
        return eulaAccepted
    }

    @SetupQuestion(
        index = 1,
        translationKey =
            "On which host should we start the gRPC Server? (Used for communicating between nodes and the services)",
        forceAnswer = false,
        answer = AddressAnswer::class,
    )
    fun q2(answer: String): Boolean {
        val valid = AddressAnswer.parseAddress(answer)
        if (valid) {
            grpcHost = answer
            return true
        } else {
            return false
        }
    }

    @SetupQuestion(
        index = 2,
        translationKey = "On which port should we start the gRPC Server?",
        forceAnswer = false,
        default = ["6565"],
    )
    fun q3(answer: String): Boolean {
        val valid = answer.ifBlank { "6565" }.trim().toIntOrNull() ?: return false
        if (valid <= 0) return false
        grpcPort = valid
        return true
    }

    @SetupQuestion(
        index = 3,
        translationKey =
            "How much memory should all services be able to use in total? (Value must be in MB)",
        forceAnswer = false,
        answer = MemorySetupAnswer::class,
    )
    fun q4(answer: String): Boolean {
        val effective = (answer.ifBlank { systemMemoryInMb.toString() }).trim()
        val value = effective.toLongOrNull() ?: return false
        if (value <= 0) return false
        if (value > systemMemoryInMb) return false
        totalAllowedMemoryMb = value
        return true
    }

    @SetupQuestion(
        index = 4,
        translationKey =
            "On which address should services bind to? (Usually it is recommended to use your public IP or 0.0.0.0, if you have IPv6 only you have to use 0.0.0.0)",
        forceAnswer = false,
        answer = AddressAnswer::class,
    )
    fun q5(answer: String): Boolean {
        val valid = AddressAnswer.parseAddress(answer)
        if (valid) {
            bindAddress = answer
            return true
        } else {
            return false
        }
    }

    @SetupQuestion(
        index = 5,
        translationKey = "What name should this node have?",
        forceAnswer = false,
        default = ["Node-1"],
    )
    fun q6(answer: String): Boolean {
        val effective = answer.ifBlank { "Node-1" }.trim()
        if (effective.length !in 3..32) return false
        nodeName = effective
        return true
    }

    @SetupQuestion(
        index = 6,
        translationKey =
            "Should the Services be configured for Velocity Modern Forwarding automatically?",
        forceAnswer = true,
        answer = BooleanSetupAnswer::class,
    )
    fun q10(answer: String): Boolean {
        modernForwardingEnabled = answer.toBoolean()
        return true
    }

    @SetupCancel
    fun cancel() {
        runBlocking { NodeShutdown.shutdown() }
    }

    @SetupFinish
    fun finish() {
        Node.instance.configProvider.updateConfig(
            NodeConfig(
                nodeName,
                UUID.randomUUID(),
                grpcPort,
                grpcHost,
                bindAddress,
                totalAllowedMemoryMb.toInt(),
                docker = DockerConfig(),
                modernForwardingEnabled,
            )
        )

        Node.instance.terminal.changePrompt("")
    }
}
