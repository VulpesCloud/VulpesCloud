package de.vulpescloud.api.mysql

import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.slf4j.LoggerFactory

object SqlLogger : SqlLogger {
    private val logger = LoggerFactory.getLogger(this::class.java)
    override fun log(context: StatementContext, transaction: Transaction) {
        logger.debug("&cMySQL &8>> &m${context.expandArgs(transaction)}")
    }
}