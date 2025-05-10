package de.vulpescloud.node.setup.setups

import de.vulpescloud.api.lang.Languages
import de.vulpescloud.api.lang.Translator
import de.vulpescloud.node.Node
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.annotations.SetupCancel
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import de.vulpescloud.node.setup.answers.BooleanSetupAnswer
import de.vulpescloud.node.setup.answers.LanguageSetupAnswer
import de.vulpescloud.node.terminal.JLineTerminal
import java.util.*
import kotlin.concurrent.withLock
import kotlin.properties.Delegates
import kotlin.system.exitProcess

@Suppress("Unused")
class FirstSetup(
    private val terminal: JLineTerminal,
    private val translator: Translator,
    private val config: NodeConfig,
    private val node: Node,
) : Setup {

    private lateinit var name: String

    private lateinit var redisUser: String
    private lateinit var redisHostname: String
    private lateinit var redisPassword: String
    private var redisPort by Delegates.notNull<Int>()

    private lateinit var mysqlUser: String
    private lateinit var mysqlPassword: String
    private lateinit var mysqlDatabase: String
    private lateinit var mysqlHostname: String
    private var mysqlPort by Delegates.notNull<Int>()
    private var mysqlSSL by Delegates.notNull<Boolean>()

    private lateinit var language: Languages

    @SetupQuestion(0, "SETUP.first-setup.QUESTION.language", LanguageSetupAnswer::class, true)
    fun setLanguage(language: String): Boolean {
        this.language = Languages.valueOf(language)
        terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.language.SUCCESS"))

        translator.setLang(this.language)
        translator.loadFromDefaultClassPath()

        return true
    }

    @SetupQuestion(1, "SETUP.first-setup.QUESTION.name")
    fun name(name: String): Boolean {
        if (name.length > 16) {
            terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.name.ANSWER.LONG"))
            return false
        }
        if (name.isEmpty()) {
            terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.name.ANSWER.EMPTY"))
            return false
        }
        this.name = name
        terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.name.ANSWER.SUCCESS"))
        return true
    }

    //
    //   REDIS
    //

    @SetupQuestion(2, "SETUP.first-setup.QUESTION.redis.user", default = ["default"])
    fun redisUser(user: String): Boolean {
        this.redisUser = user
        terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.redis.user.SUCCESS"))
        return true
    }

    @SetupQuestion(3, "SETUP.first-setup.QUESTION.redis.password")
    fun redisPassword(pw: String): Boolean {
        this.redisPassword = pw
        terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.redis.password.SUCCESS"))
        return true
    }

    @SetupQuestion(4, "SETUP.first-setup.QUESTION.redis.hostname", default = ["127.0.0.1"])
    fun redisHostname(hostname: String): Boolean {
        this.redisHostname = hostname
        terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.redis.hostname.SUCCESS"))
        return true
    }

    @SetupQuestion(5, "SETUP.first-setup.QUESTION.redis.port", default = ["6379"])
    fun redisPort(port: String): Boolean {
        val int =
            try {
                port.toInt()
            } catch (e: Exception) {
                terminal.printSetup(
                    translator.trans("SETUP.first-setup.QUESTION.redis.port.INVALID")
                )
                return false
            }
        this.redisPort = int
        terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.redis.port.SUCCESS"))
        return true
    }

    //
    //   MYSQL
    //

    @SetupQuestion(6, "SETUP.first-setup.QUESTION.mysql.user", default = ["vulpescloud"])
    fun mysqlUser(user: String): Boolean {
        this.mysqlUser = user
        terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.mysql.user.SUCCESS"))
        return true
    }

    @SetupQuestion(7, "SETUP.first-setup.QUESTION.mysql.password")
    fun mysqlPassword(pw: String): Boolean {
        this.mysqlPassword = pw
        terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.mysql.password.SUCCESS"))
        return true
    }

    @SetupQuestion(8, "SETUP.first-setup.QUESTION.mysql.hostname", default = ["127.0.0.1"])
    fun mysqlHostname(hostname: String): Boolean {
        this.mysqlHostname = hostname
        terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.mysql.hostname.SUCCESS"))
        return true
    }

    @SetupQuestion(9, "SETUP.first-setup.QUESTION.mysql.port", default = ["3306"])
    fun mysqlPort(port: String): Boolean {
        val int =
            try {
                port.toInt()
            } catch (e: Exception) {
                terminal.printSetup(
                    translator.trans("SETUP.first-setup.QUESTION.mysql.port.INVALID")
                )
                return false
            }
        this.mysqlPort = int
        terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.mysql.port.SUCCESS"))
        return true
    }

    @SetupQuestion(10, "SETUP.first-setup.QUESTION.mysql.ssl", BooleanSetupAnswer::class)
    fun mysqlSSL(ssl: String): Boolean {
        var newSSL = ssl
        when (newSSL) {
            "yes" -> {
                newSSL = true.toString()
            }
            "y" -> {
                newSSL = true.toString()
            }
            "no" -> {
                newSSL = false.toString()
            }
            "n" -> {
                newSSL = false.toString()
            }
        }
        val boolean =
            try {
                newSSL.toBoolean()
            } catch (e: Exception) {
                terminal.printSetup(
                    translator.trans("SETUP.first-setup.QUESTION.mysql.ssl.INVALID")
                )
                return false
            }
        this.mysqlSSL = boolean
        terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.mysql.ssl.SUCCESS"))
        return true
    }

    @SetupQuestion(11, "SETUP.first-setup.QUESTION.mysql.database", default = ["vulpescloud"])
    fun mysqlDatabase(database: String): Boolean {
        this.mysqlDatabase = database
        terminal.printSetup(translator.trans("SETUP.first-setup.QUESTION.mysql.database.SUCCESS"))
        return true
    }

    @SetupFinish
    fun finish() {
        node.setupLock.withLock {
            config.config.update("redis.user", this.redisUser)
            config.config.update("redis.host", this.redisHostname)
            config.config.update("redis.port", this.redisPort)
            config.config.update("redis.password", this.redisPassword)

            config.config.update("mysql.user", this.mysqlUser)
            config.config.update("mysql.password", this.mysqlPassword)
            config.config.update("mysql.database", this.mysqlDatabase)
            config.config.update("mysql.host", this.mysqlHostname)
            config.config.update("mysql.port", this.mysqlPort)
            config.config.update("mysql.ssl", this.mysqlSSL)

            config.config.update("name", this.name)
            config.config.update("ranFirstSetup", true)

            config.config.update("uuid", UUID.randomUUID().toString())

            node.setupCondition.signalAll()
        }
    }

    @SetupCancel
    fun cancel() {
        terminal.close()
        exitProcess(0)
    }
}
