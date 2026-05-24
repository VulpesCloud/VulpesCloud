package de.vulpescloud.node.command

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.collect.Iterables
import de.vulpescloud.node.command.annotation.Alias
import de.vulpescloud.node.command.annotation.Description
import de.vulpescloud.node.command.annotation.SpecificCommandSource
import org.incendo.cloud.Command
import org.incendo.cloud.CommandManager
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.annotations.BuilderModifier
import org.incendo.cloud.execution.CommandResult
import org.incendo.cloud.key.CloudKey
import org.incendo.cloud.meta.CommandMeta
import org.incendo.cloud.permission.PredicatePermission
import org.incendo.cloud.processors.cache.CaffeineCache
import org.incendo.cloud.processors.confirmation.ConfirmationConfiguration
import org.incendo.cloud.processors.confirmation.ConfirmationManager
import org.incendo.cloud.processors.confirmation.annotation.ConfirmationBuilderModifier
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger("CommandProvider")

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
    }

    fun execute(
        source: CommandSource,
        input: String,
    ): CompletableFuture<CommandResult<CommandSource>> {

        return commandManager.commandExecutor().executeCommand(source, input).exceptionally {
            exception ->
            throw exception
        }
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
