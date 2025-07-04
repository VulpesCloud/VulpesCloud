package de.vulpescloud.api.virtualconfig

import de.vulpescloud.api.mysql.VirtualConfigTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object VirtualConfigProvider {

    private val configs = mutableMapOf<String, VirtualConfig>()

    fun getConfig(name: String, description: String = "<none>", database: Database? = null): VirtualConfig {
        return if (configs.containsKey(name)) {
            configs[name]!!
        } else {
            val config = VirtualConfig(name, description, database)
            config.init()
            configs[name] = config
            config
        }
    }

    fun getAllConfigNames(database: Database? = null): List<String> {
        return transaction(database) { VirtualConfigTable.selectAll().map { it[VirtualConfigTable.name] } }
    }

    fun getAllConfigs(database: Database? = null): List<VirtualConfig> {
        val configs = mutableListOf<VirtualConfig>()
        transaction(database) {
            VirtualConfigTable.selectAll().forEach {
                val config = getConfig(it[VirtualConfigTable.name])
                configs.add(config)
            }
        }
        return configs
    }

    fun unregisterConfig(name: String) {
        configs.remove(name)
    }

    fun getAllLocalConfigs(): List<VirtualConfig> {
        return configs.values.toList()
    }
}
