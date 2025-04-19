package de.vulpescloud.node.mysql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.vulpescloud.api.mysql.TaskTable
import de.vulpescloud.node.config.NodeConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class DatabaseProvider(private val config: NodeConfig) {

    private lateinit var dataSource: HikariDataSource
    private val logger = LoggerFactory.getLogger(DatabaseProvider::class.java)

    fun initialize() {
        logger.info("Trying to connect to MySQL database...")
        val hikariConfig = HikariConfig()

        hikariConfig.jdbcUrl =
            "jdbc:mariadb://${config.mysql().host}:${config.mysql().port}/${config.mysql().database}?serverTimezone=UTC"
        hikariConfig.username = config.mysql().user
        hikariConfig.password = config.mysql().password
        hikariConfig.driverClassName = "org.mariadb.jdbc.Driver"
        hikariConfig.maxLifetime = 9223372036854775807

        dataSource = HikariDataSource(hikariConfig)


        Database.connect(dataSource)

        logger.info("Successfully connected to MySQL database.")
    }

    fun generateTables() {
        transaction {
            SchemaUtils.create(TaskTable)
        }
    }

    fun close() {
        logger.info("Closing MySQL database connection...")
        dataSource.close()
    }
}
