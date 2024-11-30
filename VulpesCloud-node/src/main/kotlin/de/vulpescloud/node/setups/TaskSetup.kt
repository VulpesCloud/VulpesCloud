/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.node.setups

import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.api.tasks.builder.TaskCreateMessageBuilder
import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.node.Node
import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import de.vulpescloud.node.setup.answer.BooleanSetupAnswer
import de.vulpescloud.node.setup.answer.MemorySetupAnswer
import de.vulpescloud.node.setup.answer.VersionSetupAnswer
import de.vulpescloud.node.task.TaskImpl
import de.vulpescloud.node.version.Version
import kotlin.math.sign
import kotlin.properties.Delegates

class TaskSetup : Setup {

    private lateinit var name: String
    private lateinit var version: Version
    private var memory by Delegates.notNull<Int>()
    private var startPort by Delegates.notNull<Int>()
    private var maxPlayers by Delegates.notNull<Int>()
    private var static by Delegates.notNull<Boolean>()
    private var fallback by Delegates.notNull<Boolean>()
    private var minServiceCount by Delegates.notNull<Int>()
    private var maintenance by Delegates.notNull<Boolean>()

    @SetupQuestion(0, "node.setup.task.question.name")
    fun name(name: String): Boolean {
        if (name.length > 16) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.name.long"))
            return false
        }
        if (name.isEmpty()) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.name.empty"))
            return false
        }
        this.name = name
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.name.success"))
        return true
    }

    @SetupQuestion(1, "node.setup.task.question.version.name", VersionSetupAnswer::class)
    fun choseVersion(version: String): Boolean {
        val ver = Node.versionProvider.search(version)

        if (ver != null) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.version.version.success"))
            this.version = ver
            return true
        } else {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.version.version.invalid"))
            return false
        }
    }

    @SetupQuestion(2, "node.setup.task.question.memory", MemorySetupAnswer::class)
    fun choseMemory(mem: String): Boolean {
        val int = try {
            mem.toInt()
        } catch (e: Exception) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.memory.invalid"))
            return false
        }
        this.memory = int
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.memory.success"))
        return true
    }

    @SetupQuestion(3, "node.setup.task.question.startPort")
    fun chosePort(port: String): Boolean {
        val int = try {
            port.toInt()
        } catch (e: Exception) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.startPort.invalid"))
            return false
        }

        if (int < 20000) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.startPort.tooSmall"))
            return false
        } else {
            this.startPort = int
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.startPort.success"))
            return true
        }
    }

    @SetupQuestion(4, "node.setup.task.question.maxPlayers")
    fun maxPlayers(maxP: String): Boolean {
        val int = try {
            maxP.toInt()
        } catch (e: Exception) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.maxPlayers.invalid"))
            return false
        }
        this.maxPlayers = int
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.maxPlayers.success"))
        return true
    }

    @SetupQuestion(5, "node.setup.task.question.static", BooleanSetupAnswer::class)
    fun isStatic(bool: String): Boolean {
        val boolean = try {
            bool.toBoolean()
        } catch (e: Exception) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.static.invalid"))
            return false
        }
        this.static = boolean
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.static.success"))
        return true
    }

    @SetupQuestion(6, "node.setup.task.question.fallback", BooleanSetupAnswer::class)
    fun fallback(bool: String): Boolean {
        if (this.version.type == VersionType.PROXY) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.fallback.override"))
            this.fallback = false
            return true
        }

        val boolean = try {
            bool.toBoolean()
        } catch (e: Exception) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.fallback.invalid"))
            return false
        }
        this.fallback = boolean
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.fallback.success"))
        return true
    }

    @SetupQuestion(7, "node.setup.task.question.minServiceCount")
    fun minServiceCount(minServiceCount: String): Boolean {
        val int = try {
            minServiceCount.toInt()
        } catch (e: Exception) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.minServiceCount.invalid"))
            return false
        }
        this.minServiceCount = int
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.minServiceCount.success"))
        return true
    }

    @SetupQuestion(8, "node.setup.task.question.maintenance", BooleanSetupAnswer::class)
    fun maintenance(bool: String): Boolean {
        val boolean = try {
            bool.toBoolean()
        } catch (e: Exception) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.maintenance.invalid"))
            return false
        }
        this.maintenance = boolean
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.maintenance.success"))
        return true
    }

    @SetupFinish
    fun finish() {

        val task = TaskImpl(
            name,
            memory,
            VersionInfo(version.name, version.type, version.versions[0].version), // todo Make this better xD
            listOf(name),
            listOf(Node.nodeConfig!!.name),
            maxPlayers,
            static,
            minServiceCount,
            maintenance,
            startPort,
            fallback
        )

        Node.instance!!.getRC()?.sendMessage(
            TaskCreateMessageBuilder
                .setTask(task)
                .build(),
            RedisPubSubChannels.VULPESCLOUD_TASK_CREATE.name
        )
    }
}