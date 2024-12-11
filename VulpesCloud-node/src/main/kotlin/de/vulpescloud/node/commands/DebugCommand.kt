package de.vulpescloud.node.commands

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import de.vulpescloud.node.command.source.CommandSource
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.slf4j.LoggerFactory


class DebugCommand {

    val logLevels = listOf("ERROR", "WARN", "INFO", "DEBUG")

    @Parser(suggestions = "logLevel")
    fun parseLogLevel(input: CommandInput): Level {
        val level = Level.toLevel(input.readString(), Level.INFO)
        return level
    }

    @Suggestions("logLevel")
    fun suggestLogLevel(): List<String> {
        return logLevels
    }

    @Command("debug logging <level>")
    fun setLogLevel(
        source: CommandSource,
        @Argument("level") level: Level,
    ) {
        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        if (rootLogger is Logger) {
            source.sendMessage("Setting logLevel to $level")
            rootLogger.level = level
        }
    }

}