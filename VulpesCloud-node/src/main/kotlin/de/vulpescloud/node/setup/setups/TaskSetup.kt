package de.vulpescloud.node.setup.setups

import de.vulpescloud.api.lang.Translator
import de.vulpescloud.api.mysql.TaskTable
import de.vulpescloud.api.task.Task
import de.vulpescloud.api.task.TaskProvider
import de.vulpescloud.api.version.Version
import de.vulpescloud.api.version.VersionProvider
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.annotations.SetupCancel
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import de.vulpescloud.node.setup.answers.BooleanSetupAnswer
import de.vulpescloud.node.setup.answers.MemorySetupAnswer
import de.vulpescloud.node.setup.answers.VersionSetupAnswer
import de.vulpescloud.node.terminal.JLineTerminal
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.json.JSONObject
import kotlin.properties.Delegates

@Suppress("Unused")
class TaskSetup(
    private val taskProvider: TaskProvider,
    private val translator: Translator,
    private val terminal: JLineTerminal,
    private val versionProvider: VersionProvider,
    private val config: NodeConfig
) : Setup {

    private lateinit var name: String
    private var maxMemory by Delegates.notNull<Int>()
    private var maxPlayers by Delegates.notNull<Int>()
    private var staticServices by Delegates.notNull<Boolean>()
    private var minOnlineCount by Delegates.notNull<Int>()
    private var maintenance by Delegates.notNull<Boolean>()
    private var startPort by Delegates.notNull<Int>()
    private var fallback by Delegates.notNull<Boolean>()
    private lateinit var version: Version

    @SetupQuestion(0, "SETUP.task.QUESTION.name")
    fun setName(name: String): Boolean {
        if (taskProvider.getTaskByName(name) != null) {
            terminal.printSetup(translator.trans("SETUP.task.QUESTION.name.alreadyExists"))
            return false
        }
        terminal.printSetup(translator.trans("SETUP.task.QUESTION.name.success"))
        this.name = name
        return true
    }

    @SetupQuestion(1, "SETUP.task.QUESTION.maxPlayers")
    fun setMaxPlayers(maxPlayerString: String): Boolean {
        val maxPlayers =
            try {
                maxPlayerString.toInt()
            } catch (e: Exception) {
                terminal.printSetup(translator.trans("SETUP.task.QUESTION.maxPlayers.invalid"))
                return false
            }
        terminal.printSetup(translator.trans("SETUP.task.QUESTION.maxPlayers.success"))
        this.maxPlayers = maxPlayers
        return true
    }

    @SetupQuestion(2, "SETUP.task.QUESTION.staticServices", BooleanSetupAnswer::class)
    fun setStatic(staticString: String): Boolean {
        var newStatic = staticString
        when (newStatic) {
            "yes" -> {
                newStatic = true.toString()
            }
            "y" -> {
                newStatic = true.toString()
            }
            "no" -> {
                newStatic = false.toString()
            }
            "n" -> {
                newStatic = false.toString()
            }
        }
        val boolean =
            try {
                newStatic.toBoolean()
            } catch (e: Exception) {
                terminal.printSetup(translator.trans("SETUP.task.QUESTION.staticServices.INVALID"))
                return false
            }
        this.staticServices = boolean
        terminal.printSetup(translator.trans("SETUP.task.QUESTION.staticServices.SUCCESS"))
        return true
    }

    @SetupQuestion(3, "SETUP.task.QUESTION.minOnlineCount")
    fun setMinCount(minCountString: String): Boolean {
        val minCount =
            try {
                minCountString.toInt()
            } catch (e: Exception) {
                terminal.printSetup(translator.trans("SETUP.task.QUESTION.minOnlineCount.INVALID"))
                return false
            }
        terminal.printSetup(translator.trans("SETUP.task.QUESTION.minOnlineCount.SUCCESS"))
        this.minOnlineCount = minCount
        return true
    }

    @SetupQuestion(4, "SETUP.task.QUESTION.maintenance", BooleanSetupAnswer::class)
    fun setMaintenance(maintenanceString: String): Boolean {
        var newMt = maintenanceString
        when (newMt) {
            "yes" -> {
                newMt = true.toString()
            }
            "y" -> {
                newMt = true.toString()
            }
            "no" -> {
                newMt = false.toString()
            }
            "n" -> {
                newMt = false.toString()
            }
        }
        val boolean =
            try {
                newMt.toBoolean()
            } catch (e: Exception) {
                terminal.printSetup(translator.trans("SETUP.task.QUESTION.maintenance.INVALID"))
                return false
            }
        this.maintenance = boolean
        terminal.printSetup(translator.trans("SETUP.task.QUESTION.maintenance.SUCCESS"))
        return true
    }

    @SetupQuestion(5, "SETUP.task.QUESTION.startPort", default = ["25565", "26625"])
    fun setStartPort(startPortString: String): Boolean {
        val startPort =
            try {
                startPortString.toInt()
            } catch (e: Exception) {
                terminal.printSetup(translator.trans("SETUP.task.QUESTION.startPort.invalid"))
                return false
            }
        terminal.printSetup(translator.trans("SETUP.task.QUESTION.startPort.success"))
        this.startPort = startPort
        return true
    }

    @SetupQuestion(6, "SETUP.task.QUESTION.maxMemory", MemorySetupAnswer::class)
    fun setMaxMemory(maxMemoryString: String): Boolean {
        val maxMem =
            try {
                maxMemoryString.toInt()
            } catch (e: Exception) {
                terminal.printSetup(translator.trans("SETUP.task.QUESTION.maxMemory.invalid"))
                return false
            }
        terminal.printSetup(translator.trans("SETUP.task.QUESTION.maxMemory.success"))
        this.maxMemory = maxMem
        return true
    }

    @SetupQuestion(7, "SETUP.task.QUESTION.version", VersionSetupAnswer::class, true)
    fun setVersion(version: String): Boolean {
        val ver = versionProvider.getVersionByName(version)

        if (ver != null) {
            terminal.printSetup(translator.trans("SETUP.task.QUESTION.version.success"))
            this.version = ver
            return true
        } else {
            terminal.printSetup(translator.trans("SETUP.task.QUESTION.version.invalid"))
            return false
        }
    }

    @SetupFinish
    fun finish() {

        fallback = version.type == VersionType.SERVER && !taskProvider.tasks().none { it.version.name != "Velocity" }

        val task = Task(
            name,
            listOf(config.name()),
            listOf(name),
            maxMemory,
            maxPlayers,
            staticServices,
            minOnlineCount,
            0,
            emptyList(),
            maintenance,
            startPort,
            fallback,
            version.versions.minByOrNull { it.version }!!,
            false,
        )

        val taskJson = JSONObject(task)
        transaction {
            val existing = TaskTable.select(TaskTable.name eq task.name).singleOrNull()

            if (existing != null) {
                TaskTable.update({ TaskTable.name eq task.name }) { it[json] = taskJson.toString() }
            } else {
                TaskTable.insert {
                    it[name] = task.name
                    it[json] = taskJson.toString()
                }
            }
        }
        getRC()?.setHashField("VULPESCLOUD_TASKS", task.name, taskJson.toString())
    }

    @SetupCancel
    fun cancel() {

    }
}
