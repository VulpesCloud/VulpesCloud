package io.github.thecguygithub.node.networking.mysql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import java.sql.ResultSet

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
            nodeConfig.mysql_host,
            nodeConfig.mysql_port,
            nodeConfig.mysql_database
        )

        config.username = nodeConfig.mysql_user
        config.password = nodeConfig.mysql_password
        config.driverClassName = "org.mariadb.jdbc.Driver"
        config.maxLifetime = 9223372036854775807

        logger.debug("Setting DataSource")

        dataSource = HikariDataSource(config)

        logger.info("Successfully established Database connection!")

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