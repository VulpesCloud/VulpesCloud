package de.vulpescloud.node.networking.mysql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.vulpescloud.node.Node
import de.vulpescloud.node.logging.Logger
import java.sql.ResultSet
import java.util.*

class MySQLController {

    private val logger = Logger()

    private var dataSource: HikariDataSource

    init {

        val nodeConfig = Node.nodeConfig!!
        val config = HikariConfig()

        logger.info("Trying to connect to MySQL Database!")

        val CONNECT_URL_FORMAT: String = "jdbc:mariadb://%s:%d/%s?serverTimezone=UTC"

        logger.debug("Setting Database information!")

        config.jdbcUrl = String.format(
            CONNECT_URL_FORMAT,
            nodeConfig.mysql!!.host,
            nodeConfig.mysql!!.port,
            nodeConfig.mysql!!.database
        )

        config.username = nodeConfig.mysql!!.user
        config.password = nodeConfig.mysql!!.password
        config.driverClassName = "org.mariadb.jdbc.Driver"
        config.maxLifetime = 9223372036854775807

        logger.debug("Setting DataSource")

        dataSource = HikariDataSource(config)

        logger.info("Successfully established Database connection!")

    }

    fun createDefaultTables() {

        logger.warn(UUID.randomUUID())

        try {
            dbExecute("CREATE TABLE IF NOT EXISTS nodes (id SERIAL PRIMARY KEY, name TEXT, uuid VARCHAR(36))")
        } catch (e: Exception) {
            logger.error(e)
        }

    }

    fun dbExecWithRs(sql: String): ResultSet {
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                val resultSet = statement.executeQuery()
                return resultSet
            }
        }
    }

    fun dbExecute(sql: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.executeUpdate()
            }
        }
    }

    fun closedb() {
        dataSource.close()
    }

}