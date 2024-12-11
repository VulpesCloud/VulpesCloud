package de.vulpescloud.api.mysql.tables

import org.jetbrains.exposed.sql.Table

object NodeTable : Table("nodes") {
    val id = integer("id").autoIncrement()
    val name = text("name")
    val uuid = varchar("uuid", 36)

    override val primaryKey = PrimaryKey(id)
}