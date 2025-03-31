package de.vulpescloud.node.commands

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import de.vulpescloud.api.language.Translator
import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.api.services.Service
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.source.CommandSource
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.processors.confirmation.annotation.Confirmation
import org.slf4j.LoggerFactory
import java.util.stream.Stream

@Suppress("UNUSED")
class DebugCommand {

    @Parser(suggestions = "services")
    fun serviceParser(input: CommandInput): Service {
        val command = input.readString()
        val task = Node.instance.serviceProvider.findServiceByName(command) ?: throw IllegalArgumentException(Translator.trans("node.commands.service.not-exist"))

        return task
    }

    @Suggestions("services")
    fun suggestServices(): Stream<String> {
        return Node.instance.serviceProvider.services().stream().map { it.name() }
    }

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

    @Command("debug test logging")
    fun testLogLevel(
        source: CommandSource,
    ) {
        val logger = LoggerFactory.getLogger("dbgCommand")

        logger.error("TEST -> &cERROR")
        logger.warn("TEST -> &cWARN")
        logger.info("TEST -> &cINFO")
        logger.debug("TEST -> &cDEBUG")
        logger.trace("TEST -> &mTRACE")
        source.sendMessage("Test logging done with ERROR, WARN, INFO, DEBUG, TRACE!")
    }

    @Confirmation
    @Command("debug remove services <service>")
    fun forceRemoveService(
        source: CommandSource,
        @Argument("service") service: Service
    ) {
        source.sendMessage("Removing service from Redis")
        Node.instance.getRC()?.deleteHashField(RedisHashNames.VULPESCLOUD_SERVICES.name, service.name())
    }

}