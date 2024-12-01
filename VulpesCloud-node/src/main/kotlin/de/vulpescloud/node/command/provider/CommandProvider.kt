/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.node.command.provider

import com.google.common.collect.Iterables
import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.node.command.CloudCommandManager
import de.vulpescloud.node.command.annotations.Alias
import de.vulpescloud.node.command.annotations.Description
import de.vulpescloud.node.command.source.CommandSource
import org.incendo.cloud.Command
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.annotations.BuilderModifier
import org.incendo.cloud.execution.CommandResult
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.key.CloudKey
import org.incendo.cloud.meta.CommandMeta
import org.incendo.cloud.parser.ParserParameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Function
import kotlin.collections.HashSet


class CommandProvider {

    val ALIAS_KEY: CloudKey<out HashSet<String>>? = CloudKey.of("vulpescloud:alias", HashSet<String>()::class.java)
    val DESCRIPTION_KEY: CloudKey<String> = CloudKey.of(
        "vulpescloud:description",
        String::class.java
    )

    val logger: Logger = LoggerFactory.getLogger(CommandProvider::class.java)

    var commandManager: CloudCommandManager? = CloudCommandManager(ExecutionCoordinator.simpleCoordinator())

    var registeredCommands: MutableList<CommandInfo>? = mutableListOf()

    private var annotationParser: AnnotationParser<CommandSource>? = null

    init {

        this.annotationParser = AnnotationParser(
            this.commandManager!!,
            CommandSource::class.java
        ) { CommandMeta.empty() }

//        this.annotationParser!!.registerBuilderModifier<Alias>(
//            Alias::class.java
//        ) { alias: Alias, builder: Command.Builder<CommandSource?> ->
//            builder.meta<Set<String>>(
//                this.ALIAS_KEY, HashSet(
//                    listOf(*alias.alias)
//                )
//            )
//        }

//        annotationParser!!.registerBuilderModifier(
//            Alias::class.java
//        ) { alias: Alias, builder: Command.Builder<CommandSource?> ->
//            builder.meta(
//                ALIAS_KEY,
//                setOf("")
//            )
//        }

        this.annotationParser!!.registerBuilderModifier(
            Description::class.java,
            BuilderModifier<Description, CommandSource?> registerBuilderModifier@{ description: Description, builder: Command.Builder<CommandSource?> ->
                if (description.description.trim { it <= ' ' }.isNotEmpty()) {
                    return@registerBuilderModifier builder.meta<String>(
                        this.DESCRIPTION_KEY,
                        description.description
                    )
                }
                builder
            })

    }

    fun suggest(source: CommandSource, input: String): List<String> {
        return listOf()
    }

    fun execute(source: CommandSource, input: String): CompletableFuture<CommandResult<CommandSource>>? {
        return commandManager?.commandExecutor()?.executeCommand(source, input)

    }

    fun register(command: Any) {
        val cloudCommand = Iterables.getFirst(
            annotationParser!!.parse(command), null
        )

        if (cloudCommand != null) {
            if (cloudCommand.nonFlagArguments().isEmpty()) {
                return
            }

            val permission = cloudCommand.commandPermission().permissionString()

            val description = cloudCommand.commandMeta().getOrSupplyDefault(
                DESCRIPTION_KEY
            ) { "No Description! Repleace me !" }

            val aliases = emptySet<String>()

            val name = cloudCommand.nonFlagArguments().first().name().lowercase()

            registeredCommands!!.add(
                CommandInfo(name, aliases, description, this.commandUsageOfRoot(name), permission)
            )
        }
    }

    fun command(name: String): CommandInfo? {
        val lowerCaseInput = name.lowercase()
        for (command in registeredCommands!!) {
            if (command.name == lowerCaseInput || command.aliases.contains(lowerCaseInput)) {
                return command
            }
        }
        return null
    }

    fun commands(): MutableCollection<CommandInfo>? {
        return this.registeredCommands?.let { Collections.unmodifiableCollection(it) }
    }

    private fun commandUsageOfRoot(root: String): List<String> {
        val commandUsage: MutableList<String> = ArrayList()
        for (command in commandManager!!.commands()) {
            // the first argument is the root, check if it matches
            val arguments = command.components()
            if (arguments.isEmpty() || !arguments.first().name().equals(root, ignoreCase = true)) {
                continue
            }

            commandUsage.add(commandManager!!.commandSyntaxFormatter().apply(null, arguments, null))
        }

        commandUsage.sort()
        return commandUsage
    }


}