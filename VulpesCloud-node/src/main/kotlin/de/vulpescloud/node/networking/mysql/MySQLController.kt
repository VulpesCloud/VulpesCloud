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

package de.vulpescloud.node.networking.mysql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.vulpescloud.node.Node
import org.slf4j.LoggerFactory
import java.sql.ResultSet

class MySQLController {

    private val logger = LoggerFactory.getLogger(MySQLController::class.java)

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
        try {
            dbExecute("CREATE TABLE IF NOT EXISTS nodes (id SERIAL PRIMARY KEY, name TEXT, uuid VARCHAR(36))")
        } catch (e: Exception) {
            logger.error(e.toString())
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