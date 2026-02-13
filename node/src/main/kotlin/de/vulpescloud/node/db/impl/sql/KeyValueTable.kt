package de.vulpescloud.node.db.impl.sql

import org.jetbrains.exposed.v1.core.Table

class KeyValueTable(name: String) : Table(name) {
    val key = varchar("key", 255)
    val value = text("value")

    override val primaryKey = PrimaryKey(key)
}
