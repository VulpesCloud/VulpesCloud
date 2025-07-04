package de.vulpescloud.api.mysql

import org.jetbrains.exposed.v1.core.Table

object VirtualConfigTable : Table("virtual-configs") {
    val id = integer("id").autoIncrement()
    val name = text("name")
    val description = text("description")
    val json = text("json")
    val lastModified = long("last_modified")

    override val primaryKey = PrimaryKey(id)
}
