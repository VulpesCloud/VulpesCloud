package de.vulpescloud.node.db

import de.vulpescloud.node.Node
import de.vulpescloud.node.utils.PropertyUtils
import org.slf4j.LoggerFactory

interface DatabaseProvider {

    fun initialize()

    fun getOrCreateDatabase(name: String): Database

    fun hasDatabase(name: String): Boolean

    fun deleteDatabase(name: String)

    companion object {
        private val logger = LoggerFactory.getLogger(DatabaseProvider::class.java)
        private val availableDatabaseProviders = mutableMapOf<String, DatabaseProvider>()
        private var allowDatabaseProviderAdding = true
        private var mainDatabaseProvider: DatabaseProvider? = null

        fun addDatabaseProvider(name: String, provider: DatabaseProvider) {
            if (allowDatabaseProviderAdding) {
                if (PropertyUtils.isMoreDBLogging()) logger.info("Adding Database Provider $name")
                availableDatabaseProviders[name] = provider
            } else {
                logger.error("Cannot add Database Provider $name after startup!")
            }
        }

        fun getAvailableDatabaseProviders(): Map<String, DatabaseProvider> =
            availableDatabaseProviders

        fun lockDatabaseProviderAdding() {
            allowDatabaseProviderAdding = false
            if (PropertyUtils.isMoreDBLogging()) logger.info("Locking Database Provider adding")
        }

        fun setAndInitializeMainDatabaseProvider() {
            if (PropertyUtils.isMoreDBLogging()) logger.info("Setting main Database Provider")
            mainDatabaseProvider =
                availableDatabaseProviders[Node.instance.configProvider.config.databaseType]
                    ?: throw IllegalStateException(
                        "Invalid database type: ${Node.instance.configProvider.config.databaseType}."
                    )
            if (PropertyUtils.isMoreDBLogging()) logger.info("Initializing main Database Provider")
            mainDatabaseProvider!!.initialize()
        }

        fun getMainDatabaseProvider(): DatabaseProvider {
            return mainDatabaseProvider
                ?: throw IllegalStateException("No main Database Provider set!")
        }
    }
}
