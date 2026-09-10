/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.db

import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.utils.PropertyUtils

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
