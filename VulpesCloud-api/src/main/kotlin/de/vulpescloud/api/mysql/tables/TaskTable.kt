package de.vulpescloud.api.mysql.tables

import org.jetbrains.exposed.sql.Table

object TaskTable : Table("nodes") {
    val id = integer("id").autoIncrement()
    val name = text("name")
    val uuid = text("json")

    override val primaryKey = PrimaryKey(id)
}