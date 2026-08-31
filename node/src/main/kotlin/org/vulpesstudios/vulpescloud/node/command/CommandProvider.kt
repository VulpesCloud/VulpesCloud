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

package org.vulpesstudios.vulpescloud.node.command

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.collect.Iterables
import org.incendo.cloud.Command
import org.incendo.cloud.CommandManager
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.annotations.BuilderModifier
import org.incendo.cloud.exception.*
import org.incendo.cloud.execution.CommandResult
import org.incendo.cloud.key.CloudKey
import org.incendo.cloud.meta.CommandMeta
import org.incendo.cloud.permission.PredicatePermission
import org.incendo.cloud.processors.cache.CaffeineCache
import org.incendo.cloud.processors.confirmation.ConfirmationConfiguration
import org.incendo.cloud.processors.confirmation.ConfirmationManager
import org.incendo.cloud.processors.confirmation.annotation.ConfirmationBuilderModifier
import org.vulpesstudios.vulpescloud.node.command.annotation.Alias
import org.vulpesstudios.vulpescloud.node.command.annotation.Description
import org.vulpesstudios.vulpescloud.node.command.annotation.SpecificCommandSource
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

class CommandProvider {

    private val aliasKey: CloudKey<Array<String>>? =
        CloudKey.of("vulpescloud:alias", Array<String>::class.java)
    private val descriptionKey: CloudKey<String> =
        CloudKey.of("vulpescloud:description", String::class.java)
    private val specificCommandSourceKey: CloudKey<KClass<*>> =
        CloudKey.of("vulpescloud:specificCommandSource", KClass::class.java)

    private var registeredCommands: MutableList<CommandInfo>? = mutableListOf()
    private var annotationParser: AnnotationParser<CommandSource>? = null

    val commandManager: CommandManager<CommandSource> = CloudCommandManager()

    fun initialize() {
        this.annotationParser =
            AnnotationParser(this.commandManager, CommandSource::class.java) { CommandMeta.empty() }

        this.annotationParser!!.registerBuilderModifier(
            Alias::class.java,
            BuilderModifier registerBuilderModifier@{
                alias: Alias,
                builder: Command.Builder<CommandSource> ->
                if (alias.alias.isNotEmpty()) {
                    return@registerBuilderModifier builder.meta(this.aliasKey!!, alias.alias)
                }
                builder
            },
        )

        this.annotationParser!!.registerBuilderModifier(
            Description::class.java,
            BuilderModifier registerBuilderModifier@{
                description: Description,
                builder: Command.Builder<CommandSource> ->
                if (description.description.trim { it <= ' ' }.isNotEmpty()) {
                    return@registerBuilderModifier builder.meta(
                        this.descriptionKey,
                        if (description.translatable) {
                            "Currently no Translator"
                            // translator.trans(description.description)
                        } else {
                            description.description
                        },
                    )
                }
                builder
            },
        )

        this.annotationParser!!.registerBuilderModifier(
            SpecificCommandSource::class.java
        ) { annotation, builder ->
            builder.permission(
                PredicatePermission.of { sender ->
                    annotation.value.java.isInstance(sender)
                }
            ).meta(this.specificCommandSourceKey, annotation.value)
        }



        ConfirmationBuilderModifier.install(this.annotationParser!!)
        val confirmationManager =
            ConfirmationManager.confirmationManager(
                ConfirmationConfiguration.builder<CommandSource>()
                    .cache(
                        CaffeineCache.of(
                            Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build()
                        )
                    )
                    .noPendingCommandNotifier {
                        it!!.sendMessage("There are no Commands that need confirmation")
                    }
                    .confirmationRequiredNotifier { source, _ ->
                        source!!.sendMessage(
                            "Type 'confirm' in the next 30 seconds to confirm this command"
                        )
                    }
                    .build()
            )
        this.commandManager.registerCommandPostProcessor(confirmationManager.createPostprocessor())
        this.commandManager.command(
            this.commandManager
                .commandBuilder("confirm")
                .handler(confirmationManager.createExecutionHandler())
        )

        registeredCommands!!.add(CommandInfo("confirm", setOf(), "", listOf()))

        this.commandManager.exceptionController().registerHandler(NoSuchCommandException::class.java) { ctx ->
            ctx.context().sender().sendError("Unknown command: ${ctx.exception().suppliedCommand()}")
        }

        this.commandManager.exceptionController().registerHandler(InvalidSyntaxException::class.java) { ctx ->
            ctx.context().sender().sendError("Invalid syntax: /${ctx.exception().correctSyntax()}")
        }

        this.commandManager.exceptionController().registerHandler(NoPermissionException::class.java) { ctx ->
            ctx.context().sender().sendError("You don't have permission to do that.")
        }

        this.commandManager.exceptionController().registerHandler(InvalidCommandSenderException::class.java) { ctx ->
            ctx.context().sender().sendError(ctx.exception().message ?: "You can't use this command here.")
        }

        this.commandManager.exceptionController().registerHandler(ArgumentParseException::class.java) { ctx ->
            // .cause is the actual parser failure (NumberFormatException, etc.)
            val cause = ctx.exception().cause
            ctx.context().sender().sendError("Invalid argument: ${cause.message ?: ctx.exception().message}")
        }

        this.commandManager.exceptionController().registerHandler(CommandExecutionException::class.java) { ctx ->
            val cause = ctx.exception().cause ?: ctx.exception()
            ctx.context().sender().sendError("An internal error occurred while executing the command.")
            cause.printStackTrace() // log it, don't just swallow it
        }
    }

    fun execute(
        source: CommandSource,
        input: String,
    ): CompletableFuture<CommandResult<CommandSource>> {

        return commandManager.commandExecutor().executeCommand(source, input)
    }

    fun register(command: Any) {
        val cloudCommand = Iterables.getFirst(annotationParser!!.parse(command), null)

        if (cloudCommand != null) {
            if (cloudCommand.nonFlagArguments().isEmpty()) {
                return
            }

            val description =
                cloudCommand.commandMeta().getOrSupplyDefault(descriptionKey) { "No Description!" }

            val aliases =
                cloudCommand.commandMeta().getOrSupplyDefault(
                    aliasKey ?: CloudKey.of("vulpescloud:alias", Array<String>::class.java)
                ) {
                    emptyArray()
                }

            val name = cloudCommand.nonFlagArguments().first().name().lowercase()

            registeredCommands!!.add(
                CommandInfo(name, aliases.toSet(), description, this.commandUsageOfRoot(name))
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

    fun commandUsageOfRoot(root: String): List<String> {
        val commandUsage: MutableList<String> = ArrayList()
        for (command in commandManager.commands()) {
            // the first argument is the root, check if it matches
            val arguments = command.components()
            if (arguments.isEmpty() || !arguments.first().name().equals(root, ignoreCase = true)) {
                continue
            }

            commandUsage.add(commandManager.commandSyntaxFormatter().apply(null, arguments, null))
        }

        commandUsage.sort()
        return commandUsage
    }
}
