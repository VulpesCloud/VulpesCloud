package de.vulpescloud.node.setups

import de.vulpescloud.api.language.Translator
import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.node.Node
import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import de.vulpescloud.node.setup.answer.BooleanSetupAnswer
import de.vulpescloud.node.setup.answer.MemorySetupAnswer
import de.vulpescloud.node.setup.answer.VersionSetupAnswer
import de.vulpescloud.node.tasks.TaskFactory.createTask
import de.vulpescloud.node.tasks.TaskImpl
import de.vulpescloud.node.version.Version
import org.json.JSONObject
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
    private lateinit var ver: String

    @SetupQuestion(0, "node.setup.task.question.name")
    fun name(name: String): Boolean {
        if (name.length > 16) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.name.long"))
            return false
        }
        if (name.isEmpty()) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.name.empty"))
            return false
        }
        this.name = name
        Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.name.success"))
        return true
    }

    @SetupQuestion(1, "node.setup.task.question.environment.name", VersionSetupAnswer::class)
    fun choseVersion(version: String): Boolean {
        val ver = Node.instance.versionProvider.getByName(version)

        if (ver != null) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.environment.success"))
            this.version = ver
            return true
        } else {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.environment.invalid"))
            return false
        }
    }

    @SetupQuestion(2, "node.setup.task.question.memory", MemorySetupAnswer::class)
    fun choseMemory(mem: String): Boolean {
        val int = try {
            mem.toInt()
        } catch (e: Exception) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.memory.invalid"))
            return false
        }
        this.memory = int
        Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.memory.success"))
        return true
    }

    @SetupQuestion(3, "node.setup.task.question.startPort")
    fun chosePort(port: String): Boolean {
        val int = try {
            port.toInt()
        } catch (e: Exception) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.startPort.invalid"))
            return false
        }

        if (int < 20000) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.startPort.tooSmall"))
            return false
        } else {
            this.startPort = int
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.startPort.success"))
            return true
        }
    }

    @SetupQuestion(4, "node.setup.task.question.maxPlayers")
    fun maxPlayers(maxP: String): Boolean {
        val int = try {
            maxP.toInt()
        } catch (e: Exception) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.maxPlayers.invalid"))
            return false
        }
        this.maxPlayers = int
        Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.maxPlayers.success"))
        return true
    }

    @SetupQuestion(5, "node.setup.task.question.static", BooleanSetupAnswer::class, true)
    fun isStatic(bool: String): Boolean {
        val boolean = try {
            bool.toBoolean()
        } catch (e: Exception) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.static.invalid"))
            return false
        }
        this.static = boolean
        Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.static.success"))
        return true
    }

    @SetupQuestion(6, "node.setup.task.question.fallback", BooleanSetupAnswer::class, true)
    fun fallback(bool: String): Boolean {
        if (this.version.versionType == VersionType.PROXY) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.fallback.override"))
            this.fallback = false
            return true
        }

        val boolean = try {
            bool.toBoolean()
        } catch (e: Exception) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.fallback.invalid"))
            return false
        }
        this.fallback = boolean
        Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.fallback.success"))
        return true
    }

    @SetupQuestion(7, "node.setup.task.question.minServiceCount")
    fun minServiceCount(minServiceCount: String): Boolean {
        val int = try {
            minServiceCount.toInt()
        } catch (e: Exception) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.minServiceCount.invalid"))
            return false
        }
        this.minServiceCount = int
        Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.minServiceCount.success"))
        return true
    }

    @SetupQuestion(8, "node.setup.task.question.maintenance", BooleanSetupAnswer::class, true)
    fun maintenance(bool: String): Boolean {
        val boolean = try {
            bool.toBoolean()
        } catch (e: Exception) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.maintenance.invalid"))
            return false
        }
        this.maintenance = boolean
        Node.instance.terminal.printSetup(Translator.trans("node.setup.task.question.maintenance.success"))
        return true
    }

    @SetupFinish
    fun finish() {
        val task = TaskImpl(
            name,
            memory,
            VersionInfo(version.environment.name, version.versionType.name, version.versions.first().version),
            listOf(name),
            listOf(Node.instance.config.name),
            maxPlayers,
            static,
            minServiceCount,
            maintenance,
            startPort,
            fallback
        )
        Node.instance.terminal.printSetup(JSONObject(task).toString(4))
        createTask(JSONObject(task))
    }
}