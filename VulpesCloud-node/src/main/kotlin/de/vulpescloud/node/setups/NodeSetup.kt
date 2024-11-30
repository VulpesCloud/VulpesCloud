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

import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.config.RedisEndpointData
import de.vulpescloud.node.networking.mysql.MySQLEndpointData
import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.annotations.SetupCancel
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import de.vulpescloud.node.setup.answer.BooleanSetupAnswer
import de.vulpescloud.node.util.Configurations.writeContent
import java.nio.file.Path
import kotlin.concurrent.withLock
import kotlin.properties.Delegates

class NodeSetup : Setup {

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

    @SetupQuestion(0, "node.setup.node.question.name")
    fun name(name: String): Boolean {
        if (name.length > 16) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.name.answer.long"))
            return false
        }
        if (name.isEmpty()) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.name.answer.empty"))
            return false
        }
        this.name = name
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.name.answer.success"))
        return true
    }

    //
    //   REDIS
    //

    @SetupQuestion(1, "node.setup.node.question.redis.user")
    fun redisUser(user: String): Boolean {
        this.redisUser = user
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.redis.user.success"))
        return true
    }

    @SetupQuestion(2, "node.setup.node.question.redis.password")
    fun redisPassword(pw: String): Boolean {
        this.redisPassword = pw
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.redis.password.success"))
        return true
    }

    @SetupQuestion(3, "node.setup.node.question.redis.hostname")
    fun redisHostname(hostname: String): Boolean {
        this.redisHostname = hostname
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.redis.hostname.success"))
        return true
    }

    @SetupQuestion(4, "node.setup.node.question.redis.port")
    fun redisPort(port: String): Boolean {
        val int = try {
            port.toInt()
        } catch (e: Exception) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.redis.port.invalid"))
            return false
        }
        this.redisPort = int
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.redis.port.success"))
        return true
    }

    //
    //   MYSQL
    //

    @SetupQuestion(5, "node.setup.node.question.mysql.user")
    fun mysqlUser(user: String): Boolean {
        this.mysqlUser = user
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.mysql.user.success"))
        return true
    }

    @SetupQuestion(6, "node.setup.node.question.mysql.password")
    fun mysqlPassword(pw: String): Boolean {
        this.mysqlPassword = pw
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.mysql.password.success"))
        return true
    }

    @SetupQuestion(7, "node.setup.node.question.mysql.hostname")
    fun mysqlHostname(hostname: String): Boolean {
        this.mysqlHostname = hostname
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.mysql.hostname.success"))
        return true
    }

    @SetupQuestion(8, "node.setup.node.question.mysql.port")
    fun mysqlPort(port: String): Boolean {
        val int = try {
            port.toInt()
        } catch (e: Exception) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.mysql.port.invalid"))
            return false
        }
        this.mysqlPort = int
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.mysql.port.success"))
        return true
    }

    @SetupQuestion(9, "node.setup.node.question.mysql.ssl", BooleanSetupAnswer::class)
    fun mysqlSSL(ssl: String): Boolean {
        val boolean = try {
            ssl.toBoolean()
        } catch (e: Exception) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.mysql.ssl.invalid"))
            return false
        }
        this.mysqlSSL = boolean
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.redis.ssl.success"))
        return true
    }

    @SetupQuestion(10, "node.setup.node.question.mysql.database")
    fun mysqlDatabase(database: String): Boolean {
        this.mysqlDatabase = database
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.node.question.redis.database.success"))
        return true
    }


    @SetupFinish
    fun finish() {
        val config = Node.nodeConfig!!
        Node.instance!!.setupLock.withLock {
            config.redis = RedisEndpointData(this.redisUser, this.redisHostname, this.redisPort, this.redisPassword)
            config.mysql = MySQLEndpointData(this.mysqlUser, this.mysqlPassword, this.mysqlDatabase, this.mysqlHostname, this.mysqlPort, this.mysqlSSL)
            config.name = this.name
            Node.nodeConfig!!.ranFirstSetup = true
            writeContent(Path.of("config.json"), config)
            Node.instance!!.setupCondition.signalAll()
        }
        Node.terminal!!.commandReadingThread.interrupt()
    }

    @SetupCancel
    fun cancel() {
        NodeShutdown.nodeShutdown(true)
    }

}