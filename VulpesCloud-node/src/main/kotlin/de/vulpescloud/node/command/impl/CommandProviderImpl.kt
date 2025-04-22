package de.vulpescloud.node.command.impl

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.collect.Iterables
import de.vulpescloud.api.lang.Translator
import de.vulpescloud.node.command.CloudCommandManager
import de.vulpescloud.node.command.CommandProvider
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotations.Alias
import de.vulpescloud.node.command.annotations.Description
import org.incendo.cloud.Command
import org.incendo.cloud.CommandManager
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.annotations.BuilderModifier
import org.incendo.cloud.execution.CommandResult
import org.incendo.cloud.key.CloudKey
import org.incendo.cloud.meta.CommandMeta
import org.incendo.cloud.processors.cache.CaffeineCache
import org.incendo.cloud.processors.confirmation.ConfirmationConfiguration
import org.incendo.cloud.processors.confirmation.ConfirmationManager
import org.incendo.cloud.processors.confirmation.annotation.ConfirmationBuilderModifier
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException



class CommandProviderImpl(
    private val translator: Translator,
) : CommandProvider {

    private val aliasKey: CloudKey<Array<String>>? = CloudKey.of("vulpescloud:alias", Array<String>::class.java)
    private val descriptionKey: CloudKey<String> = CloudKey.of(
        "vulpescloud:description",
        String::class.java
    )
    private var registeredCommands: MutableList<CommandInfo>? = mutableListOf()
    private var annotationParser: AnnotationParser<CommandSource>? = null

    override val commandManager: CommandManager<CommandSource> = CloudCommandManager()
    private val logger = LoggerFactory.getLogger(CommandProviderImpl::class.java)

    override fun initialize() {
        this.annotationParser = AnnotationParser(
            this.commandManager,
            CommandSource::class.java
        ) { CommandMeta.empty() }

        this.annotationParser!!.registerBuilderModifier(
            Alias::class.java,
            BuilderModifier<Alias, CommandSource?> registerBuilderModifier@{ alias: Alias, builder: Command.Builder<CommandSource?> ->
                if (alias.alias.isNotEmpty()) {
                    return@registerBuilderModifier builder.meta(
                        this.aliasKey!!,
                        alias.alias
                    )
                }
                builder
            }
        )

        this.annotationParser!!.registerBuilderModifier(
            Description::class.java,
            BuilderModifier<Description, CommandSource?> registerBuilderModifier@{ description: Description, builder: Command.Builder<CommandSource?> ->
                if (description.description.trim { it <= ' ' }.isNotEmpty()) {
                    return@registerBuilderModifier builder.meta<String>(
                        this.descriptionKey,
                        if (description.translatable) {
                            translator.trans(description.description)
                        } else {
                            description.description
                        }
                    )
                }
                builder
            }
        )

        ConfirmationBuilderModifier.install(this.annotationParser!!)
        val confirmationManager = ConfirmationManager.confirmationManager(
            ConfirmationConfiguration.builder<CommandSource>()
                .cache(CaffeineCache.of(Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build()))
                .noPendingCommandNotifier { it!!.sendMessage("There are no Commands that need confirmation") }
                .confirmationRequiredNotifier { source, _ -> source!!.sendMessage("Type 'confirm' to confirm this command") }
                .build()
        )
        this.commandManager.registerCommandPostProcessor(confirmationManager.createPostprocessor())
        this.commandManager.command(
            this.commandManager.commandBuilder("confirm").handler(confirmationManager.createExecutionHandler())
        )

        registeredCommands!!.add(
            CommandInfo(
                "confirm",
                setOf(),
                "",
                listOf(""),
                ""
            )
        )

    }

    override fun suggest(source: CommandSource, input: String): List<String> {
        return listOf()
    }

    override fun execute(source: CommandSource, input: String): CompletableFuture<CommandResult<CommandSource>> {
        return commandManager.commandExecutor().executeCommand(source, input).exceptionally { exception: Throwable? ->
            logger.error("Exception while executing command", exception)
            throw if (exception is CompletionException) exception else CompletionException(exception)
        }
    }

    override fun register(command: Any) {
        val cloudCommand = Iterables.getFirst(
            annotationParser!!.parse(command), null
        )

        if (cloudCommand != null) {
            if (cloudCommand.nonFlagArguments().isEmpty()) {
                return
            }

            val permission = cloudCommand.commandPermission().permissionString()

            val description = cloudCommand.commandMeta().getOrSupplyDefault(
                descriptionKey
            ) { "No Description!" }

            val aliases = cloudCommand.commandMeta().getOrSupplyDefault(
                aliasKey ?: CloudKey.of("vulpescloud:alias", Array<String>::class.java)
            ) { emptyArray() }

            val name = cloudCommand.nonFlagArguments().first().name().lowercase()

            registeredCommands!!.add(
                CommandInfo(name, aliases.toSet(), description, this.commandUsageOfRoot(name), permission)
            )
        }
    }

    override fun command(name: String): CommandInfo? {
        val lowerCaseInput = name.lowercase()
        for (command in registeredCommands!!) {
            if (command.name == lowerCaseInput || command.aliases.contains(lowerCaseInput)) {
                return command
            }
        }
        return null
    }

    override fun commands(): MutableCollection<CommandInfo>? {
        return this.registeredCommands?.let { Collections.unmodifiableCollection(it) }
    }

    override fun commandUsageOfRoot(root: String): List<String> {
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