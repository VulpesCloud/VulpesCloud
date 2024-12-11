package de.vulpescloud.api.mysql.tables

import org.jetbrains.exposed.sql.Table

object TaskTable : Table("tasks") {
    val id = integer("id").autoIncrement()
    val name = text("name")
    val json = text("json")

    override val primaryKey = PrimaryKey(id)
}