package io.github.thecguygithub.node.command.provider

import io.github.thecguygithub.api.command.CommandInfo
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.Node.Companion.terminal
import io.github.thecguygithub.node.command.annotations.AliasAnnotation
import io.github.thecguygithub.node.command.annotations.Description
import io.github.thecguygithub.node.command.defaults.DefaultCommandManager
import io.github.thecguygithub.node.command.source.CommandSource
import io.github.thecguygithub.node.logging.Logger
import io.leangen.geantyref.TypeToken
import org.incendo.cloud.Command
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.annotations.BuilderModifier
import org.incendo.cloud.key.CloudKey
import org.incendo.cloud.meta.CommandMeta
import org.incendo.cloud.parser.ParserParameters
import java.util.*
import java.util.function.Function


class CommandProvider {

    val ALIAS_KEY =
        CloudKey.of("testcloud:alias", object : TypeToken<Set<String>?>() {})
    val DESCRIPTION_KEY: CloudKey<String> = CloudKey.of(
        "testcloud:description",
        String::class.java
    )

    val logger = Logger()

    var commandManager: DefaultCommandManager<CommandSource>?

    var registeredCommands: MutableList<CommandInfo>? = null

    var annotationParser: AnnotationParser<CommandSource>? = null

    init {
        terminal!!.printLine("WEPWOP")

        this.commandManager = DefaultCommandManager()

        terminal!!.printLine("WOPWEP")
        this.annotationParser = AnnotationParser<CommandSource>(
            this.commandManager!!,
            CommandSource::class.java,
            Function<ParserParameters, CommandMeta> { CommandMeta.empty() }
        )

        terminal!!.printLine("KRUEMEL MC")

        this.annotationParser!!.registerBuilderModifier<AliasAnnotation>(
            AliasAnnotation::class.java,
            BuilderModifier<AliasAnnotation, CommandSource?> { alias: AliasAnnotation, builder: Command.Builder<CommandSource?> ->
                builder.meta<Set<String>>(
                    Node.commandProvider!!.ALIAS_KEY, HashSet<String>(
                        listOf<String>(*alias.value)
                    )
                )
            })
        this.annotationParser!!.registerBuilderModifier<Description>(
            Description::class.java,
            BuilderModifier<Description, CommandSource?> registerBuilderModifier@{ description: Description, builder: Command.Builder<CommandSource?> ->
                if (description.value.trim { it <= ' ' }.isNotEmpty()) {
                    return@registerBuilderModifier builder.meta<String>(
                        Node.commandProvider!!.DESCRIPTION_KEY,
                        description.value
                    )
                }
                builder
            })

        terminal!!.printLine("ALRAM!")

        this.registeredCommands = ArrayList()


//        try {
//            logger.debug("Loading CommandManager")
//            commandManager = DefaultCommandManager()
//
//            logger.debug("Making a list!")
//
//            registeredCommands = mutableListOf()
//
//            logger.debug("Registering Commands!")
//
//            ClearCommand()
//
//            logger.debug("loading help")
//            HelpCommand()
//
//            logger.debug("loading shutdown")
//            ShutdownCommand()
//
//            logger.debug("Success!")
//        } catch (e: Exception) {
//            Logger().error(e.toString())
//        }
    }


}