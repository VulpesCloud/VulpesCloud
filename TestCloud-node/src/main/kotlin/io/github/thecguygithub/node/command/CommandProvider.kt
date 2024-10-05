package io.github.thecguygithub.node.command

import io.github.thecguygithub.node.command.CommandParser.serializer
import io.github.thecguygithub.node.commands.*
import lombok.Getter
import lombok.experimental.Accessors
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.Unmodifiable
import java.util.*


@Getter
@Accessors(fluent = true)
class CommandProvider : CommandService {
    private val commands: MutableList<Command?> = ArrayList()

    init {
        this.registerCommand(ShutdownCommand())
        this.registerCommand(InfoCommand())
        this.registerCommand(ClearCommand())
        this.registerCommand(HelpCommand())
        this.registerCommand(LogCommand())
    }

    override fun commands(): List<Command?> {
        return commands
    }

    @Contract(pure = true)
    override fun commandsByName(name: String?): @Unmodifiable MutableList<Command?>? {
        return commands.stream().filter { it: Command? ->
            it!!.name.equals(name, true) || Arrays.stream(
                it.aliases
            ).anyMatch { s -> s.equals(name, true) }
        }.toList()
    }

    override fun registerCommand(command: Command?) {
        commands.add(command)
    }

    override fun registerCommands(vararg commands: Command) {
        for (command in commands) {
            registerCommand(command)
        }
    }


    override fun unregisterCommand(command: Command?) {
        commands.remove(command)
    }

    override fun call(commandId: String?, args: Array<String?>?) {
        serializer(this, commandId!!, args)
    }
}