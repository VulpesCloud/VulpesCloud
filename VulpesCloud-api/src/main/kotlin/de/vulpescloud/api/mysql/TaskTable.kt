package de.vulpescloud.api.mysql

import org.jetbrains.exposed.v1.core.Table

object TaskTable : Table("tasks") {
    val id = integer("id").autoIncrement()
    val name = text("name")
    val json = text("json")

    override val primaryKey = PrimaryKey(id)
}
