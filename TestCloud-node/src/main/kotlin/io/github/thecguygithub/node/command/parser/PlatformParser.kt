package io.github.thecguygithub.node.command.parser

import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.command.source.CommandSource
import io.github.thecguygithub.node.platforms.Platform
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.parser.ParserDescriptor

class PlatformParser : ArgumentParser<CommandSource, Platform> {

    override fun parse(
        commandContext: CommandContext<CommandSource>,
        commandInput: CommandInput,
    ): ArgumentParseResult<Platform> {
        val input = commandInput.peekString()

        try {
            val platform = Node.platformService!!.find(input)
            if (platform != null) {
                return ArgumentParseResult.success(platform)
            } else {
                return ArgumentParseResult.failure(NullPointerException(platform))
            }
        } catch (e: Exception) {
            return ArgumentParseResult.failure(e)
        }
    }

    companion object {
        fun platformParser(): ParserDescriptor<CommandSource, Platform> {
            return ParserDescriptor.of(PlatformParser(), Platform::class.java)
        }
    }


}