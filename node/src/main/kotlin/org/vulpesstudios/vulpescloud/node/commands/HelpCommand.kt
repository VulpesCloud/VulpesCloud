/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.commands

import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.vulpesstudios.vulpescloud.node.command.CommandInfo
import org.vulpesstudios.vulpescloud.node.command.CommandProvider
import org.vulpesstudios.vulpescloud.node.command.CommandSource
import org.vulpesstudios.vulpescloud.node.command.annotation.Alias
import java.util.stream.Stream

@Suppress("UNUSED")
@Alias(["?"])
class HelpCommand(private val commandProvider: CommandProvider) {

    @Parser(suggestions = "commands")
    fun commandInfoParser(input: CommandInput): CommandInfo {
        val command = input.readString()
        val commandInfo = commandProvider.command(command) ?: throw IllegalStateException()

        return commandInfo
    }

    @Suggestions("commands")
    fun suggestCommands(): Stream<String> {
        return commandProvider.commands()!!.stream().map { it.name }
    }

    @Command("help|?")
    fun sendGeneralHelp(source: CommandSource) {
        commandProvider.commands()!!.forEach {
            source.sendMessage(" <dark_gray>»</dark_gray> <white>${it.joinNameToAliases(", ")}</white> <dark_gray>-</dark_gray> <gray>${it.description}</gray>")
        }
    }

    @Command("help|? <command>")
    fun sendSpecificHelp(source: CommandSource, @Argument("command") command: CommandInfo?) {
        if (command != null) {
            source.sendMessage("<gray>Aliases<dark_gray>:</dark_gray> <white>${command.joinNameToAliases(", ")}</white>")
            source.sendMessage("<gray>Description<dark_gray>:</dark_gray> <white>${command.description}</white>")
            source.sendMessage("<gray>Usages<dark_gray>:</dark_gray>")
            command.usage.forEach { source.sendMessage(" <dark_gray>»</dark_gray> <white>$it</white>") }
        } else {
            source.sendMessage("<red>Invalid command!</red>")
        }
    }
}