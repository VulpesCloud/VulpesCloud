package de.vulpescloud.node.setups

import de.vulpescloud.api.language.Languages
import de.vulpescloud.api.language.Translator
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.annotations.SetupCancel
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import de.vulpescloud.node.setup.answer.BooleanSetupAnswer
import de.vulpescloud.node.setup.answer.LanguageSetupAnswer
import java.util.*
import kotlin.concurrent.withLock
import kotlin.properties.Delegates

class FirstSetup : Setup {

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

    @SetupQuestion(0, "node.setup.node.question.name")
    fun name(name: String): Boolean {
        if (name.length > 16) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.name.answer.long"))
            return false
        }
        if (name.isEmpty()) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.name.answer.empty"))
            return false
        }
        this.name = name
        Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.name.answer.success"))
        return true
    }

    //
    //   REDIS
    //

    @SetupQuestion(1, "node.setup.node.question.redis.user", default = ["default"])
    fun redisUser(user: String): Boolean {
        this.redisUser = user
        Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.redis.user.success"))
        return true
    }

    @SetupQuestion(2, "node.setup.node.question.redis.password")
    fun redisPassword(pw: String): Boolean {
        this.redisPassword = pw
        Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.redis.password.success"))
        return true
    }

    @SetupQuestion(3, "node.setup.node.question.redis.hostname", default = ["127.0.0.1"])
    fun redisHostname(hostname: String): Boolean {
        this.redisHostname = hostname
        Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.redis.hostname.success"))
        return true
    }

    @SetupQuestion(4, "node.setup.node.question.redis.port", default = ["6379"])
    fun redisPort(port: String): Boolean {
        val int = try {
            port.toInt()
        } catch (e: Exception) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.redis.port.invalid"))
            return false
        }
        this.redisPort = int
        Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.redis.port.success"))
        return true
    }

    //
    //   MYSQL
    //

    @SetupQuestion(5, "node.setup.node.question.mysql.user", default = ["vulpescloud"])
    fun mysqlUser(user: String): Boolean {
        this.mysqlUser = user
        Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.mysql.user.success"))
        return true
    }

    @SetupQuestion(6, "node.setup.node.question.mysql.password")
    fun mysqlPassword(pw: String): Boolean {
        this.mysqlPassword = pw
        Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.mysql.password.success"))
        return true
    }

    @SetupQuestion(7, "node.setup.node.question.mysql.hostname", default = ["127.0.0.1"])
    fun mysqlHostname(hostname: String): Boolean {
        this.mysqlHostname = hostname
        Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.mysql.hostname.success"))
        return true
    }

    @SetupQuestion(8, "node.setup.node.question.mysql.port", default = ["3306"])
    fun mysqlPort(port: String): Boolean {
        val int = try {
            port.toInt()
        } catch (e: Exception) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.mysql.port.invalid"))
            return false
        }
        this.mysqlPort = int
        Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.mysql.port.success"))
        return true
    }

    @SetupQuestion(9, "node.setup.node.question.mysql.ssl", BooleanSetupAnswer::class)
    fun mysqlSSL(ssl: String): Boolean {
        val boolean = try {
            ssl.toBoolean()
        } catch (e: Exception) {
            Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.mysql.ssl.invalid"))
            return false
        }
        this.mysqlSSL = boolean
        Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.redis.ssl.success"))
        return true
    }

    @SetupQuestion(10, "node.setup.node.question.mysql.database", default = ["vulpescloud"])
    fun mysqlDatabase(database: String): Boolean {
        this.mysqlDatabase = database
        Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.redis.database.success"))
        return true
    }

    @SetupQuestion(11, "node.setup.node.question.language", LanguageSetupAnswer::class, true)
    fun setLanguage(language: String): Boolean {
        this.language = Languages.valueOf(language)
        Node.instance.terminal.printSetup(Translator.trans("node.setup.node.question.language.success"))
        return true
    }

    @SetupFinish
    fun finish() {
        val config = Node.instance.config.config
        Node.instance.setupLock.withLock {

            config.update("redis.user", this.redisUser)
            config.update("redis.hostname", this.redisHostname)
            config.update("redis.port", this.redisPort)
            config.update("redis.password", this.redisPassword)

            config.update("mysql.user", this.mysqlUser)
            config.update("mysql.password", this.mysqlPassword)
            config.update("mysql.database", this.mysqlDatabase)
            config.update("mysql.host", this.mysqlHostname)
            config.update("mysql.port", this.mysqlPort)
            config.update("mysql.ssl", this.mysqlSSL)

            config.update("name", this.name)
            config.update("ranFirstSetup", true)

            config.update("uuid", UUID.randomUUID().toString())

            // config.save()

            Node.instance.setupCondition.signalAll()
        }
    }

    @SetupCancel
    fun cancel() {
        NodeShutdown.forceShutdown(true)
    }

}