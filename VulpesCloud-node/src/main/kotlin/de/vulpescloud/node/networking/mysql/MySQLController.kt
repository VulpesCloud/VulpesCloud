package de.vulpescloud.node.networking.mysql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.vulpescloud.api.mysql.tables.NodeTable
import de.vulpescloud.api.mysql.tables.TaskTable
import de.vulpescloud.node.Node
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class MySQLController {

    private val dataSource: HikariDataSource
    private val loggerDB = LoggerFactory.getLogger(MySQLController::class.java)
    private val db: Database

    init {
        val nodeConfig = Node.instance.config
        val config = HikariConfig()

        loggerDB.info("Trying to connect to MySQL Database!")

        val CONNECT_URL_FORMAT = "jdbc:mariadb://%s:%d/%s?serverTimezone=UTC"

        loggerDB.debug("Setting Database information!")

        config.jdbcUrl = String.format(
            CONNECT_URL_FORMAT,
            nodeConfig.mysql.host,
            nodeConfig.mysql.port,
            nodeConfig.mysql.database
        )

        config.username = nodeConfig.mysql.user
        config.password = nodeConfig.mysql.password
        config.driverClassName = "org.mariadb.jdbc.Driver"
        config.maxLifetime = 9223372036854775807

        loggerDB.debug("Setting DataSource")

        dataSource = HikariDataSource(config)

        db = Database.connect(
            datasource = dataSource
        )

        transaction {
            addLogger(SqlInternalLogger)
        }

        loggerDB.info("Successfully established Database connection!")
    }

    fun generateDefaultTables() {
        loggerDB.debug("Generating tables...")
        transaction {
            addLogger(SqlInternalLogger)
            SchemaUtils.create(NodeTable)
            SchemaUtils.create(TaskTable)
        }
    }

    fun close() {
        loggerDB.info("Closing Database connection")
        dataSource.close()
    }

    object SqlInternalLogger : org.jetbrains.exposed.sql.SqlLogger {
        private val logger = LoggerFactory.getLogger(this::class.java)
        override fun log(context: StatementContext, transaction: Transaction) {
            logger.debug("&cMySQL &8>> &m${context.expandArgs(transaction)}")
        }
    }

}