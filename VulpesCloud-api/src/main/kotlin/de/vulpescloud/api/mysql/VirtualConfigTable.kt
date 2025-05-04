package de.vulpescloud.api.mysql

import org.jetbrains.exposed.sql.Table

object VirtualConfigTable : Table("virtual-configs") {
    val id = integer("id").autoIncrement()
    val name = text("name")
    val json = text("json")

    override val primaryKey = PrimaryKey(id)
}
