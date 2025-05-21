package de.vulpescloud.api.virtualconfig

import de.vulpescloud.api.mysql.VirtualConfigTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class VirtualConfigProvider {

    fun getConfigsFromMySQL(): List<VirtualConfig> {
        val configs = mutableListOf<VirtualConfig>()
        transaction {
            VirtualConfigTable.selectAll().forEach {
                val name = it[VirtualConfigTable.name]
                val description = it[VirtualConfigTable.description]
                val config = VirtualConfig(name, description)
                configs.add(config)
            }
        }

        return configs
    }

}
