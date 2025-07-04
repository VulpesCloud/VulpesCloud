package de.vulpescloud.wrapper.mysql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.slf4j.LoggerFactory

class DatabaseProvider(
    hostname: String,
    port: Int,
    user: String,
    password: String,
    database: String
) {
    private var database: Database
    private var dataSource: HikariDataSource
    private val logger = LoggerFactory.getLogger(DatabaseProvider::class.java)

    init {
        logger.info("Trying to connect to MySQL database...")
        val hikariConfig = HikariConfig()

        hikariConfig.jdbcUrl =
            "jdbc:mariadb://$hostname:$port/$database?serverTimezone=UTC"
        hikariConfig.username = user
        hikariConfig.password = password
        hikariConfig.driverClassName = "org.mariadb.jdbc.Driver"
        hikariConfig.maxLifetime = 9223372036854775807

        dataSource = HikariDataSource(hikariConfig)

        this.database = Database.connect(dataSource)

        logger.info("Successfully connected to MySQL database.")
    }

    fun getDatabase(): Database = database

    fun close() {
        logger.info("Closing MySQL database connection...")
        dataSource.close()
    }
}
