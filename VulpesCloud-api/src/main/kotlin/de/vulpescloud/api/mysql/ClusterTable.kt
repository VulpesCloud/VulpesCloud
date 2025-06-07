package de.vulpescloud.api.mysql

import org.jetbrains.exposed.v1.core.Table

object ClusterTable : Table("cluster") {
    val id = integer("id").autoIncrement()
    val uuid = varchar("uuid", 36)
    val name = text("name")
    val allowed = bool("allowed")

    override val primaryKey = PrimaryKey(id)
}
