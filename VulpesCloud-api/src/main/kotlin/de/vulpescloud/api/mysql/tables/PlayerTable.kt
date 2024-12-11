package de.vulpescloud.api.mysql.tables

import org.jetbrains.exposed.sql.Table

object PlayerTable : Table("players") {
    val id = integer("id").autoIncrement()
    val name = text("name")
    val uuid = varchar("uuid", 36)
    val ip = text("ip")
    val firstLogin = long("firstLogin")
    val lastLogin = long("lastLogin")

    override val primaryKey = PrimaryKey(id)
}