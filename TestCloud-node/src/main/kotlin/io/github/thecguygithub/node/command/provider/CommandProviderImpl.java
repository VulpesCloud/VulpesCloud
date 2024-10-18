package io.github.thecguygithub.node.command.provider;

import com.google.common.collect.Iterables;
import io.github.thecguygithub.api.command.CommandInfo;
import io.github.thecguygithub.node.Node;
import io.github.thecguygithub.node.command.CommandProvider;
import io.github.thecguygithub.node.command.annotations.AliasAnnotation;
import io.github.thecguygithub.node.command.annotations.Description;
import io.github.thecguygithub.node.command.defaults.DefaultCommandManager;
import io.github.thecguygithub.node.command.source.CommandSource;
import io.github.thecguygithub.node.logging.Logger;
import io.leangen.geantyref.TypeToken;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.meta.CommandMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

// @Singleton
public class CommandProviderImpl implements CommandProvider {

    private static final CloudKey<Set<String>> ALIAS_KEY = CloudKey.of("testcloud:alias", new TypeToken<Set<String>>() {});
    private static final CloudKey<String> DESCRIPTION_KEY = CloudKey.of("testcloud:description", String.class);

    private final CommandManager<CommandSource> commandManager;
    private final AnnotationParser<CommandSource> annotationParser;
    private final List<CommandInfo> registeredCommands;


    // @Inject
    public CommandProviderImpl(
            @NonNull DefaultCommandManager commandManager
    ) {

        Node.Companion.getTerminal().printLine("WEPWOP");

        this.commandManager = commandManager;

        Node.Companion.getTerminal().printLine("WOPWEP");
        this.annotationParser = new AnnotationParser<>(
                this.commandManager,
                CommandSource.class,
                _ -> CommandMeta.empty()
        );

        Node.Companion.getTerminal().printLine("KRUEMEL MC");

        this.annotationParser.registerBuilderModifier(
                AliasAnnotation.class,
                (alias, builder) -> builder.meta(ALIAS_KEY, new HashSet<>(Arrays.asList(alias.value()))));
        this.annotationParser.registerBuilderModifier(Description.class, (description, builder) -> {
            if (!description.value().trim().isEmpty()) {
                return builder.meta(DESCRIPTION_KEY, description.value());
            }
            return builder;
        });

        Node.Companion.getTerminal().printLine("ALRAM!");

        this.registeredCommands = new ArrayList<>();

    };

    @Override
    public @NonNull List<String> suggest(@NonNull CommandSource source, @NonNull String input) {
        return List.of();
    }

    @Override
    public CompletableFuture<?> execute(@NonNull CommandSource source, @NonNull String input) {
        return this.commandManager.commandExecutor().executeCommand(source, input).exceptionally(exception -> {
            throw exception instanceof CompletionException cex ? cex : new CompletionException(exception);
        });

    }

    @Override
    public void register(@NonNull Class<?> commandClass) {

    }

    @Override
    public void register(@NonNull Object command) {

        var cloudCommand = Iterables.getFirst(this.annotationParser.parse(command), null);

        if (cloudCommand != null) {
            if (cloudCommand.nonFlagArguments().isEmpty()) {
                return;
            }

            var permission = cloudCommand.commandPermission().permissionString();

            var description = cloudCommand.commandMeta().get(DESCRIPTION_KEY);

            var aliases = cloudCommand.commandMeta().getOrDefault(ALIAS_KEY, Collections.emptySet());

            var name = cloudCommand.nonFlagArguments().getFirst().name().toLowerCase(Locale.ROOT);

            this.registeredCommands.add(
                    new CommandInfo(name, aliases, description, this.commandUsageOfRoot(name))
            );
        }

    }

    @Override
    public @Nullable CommandInfo command(@NonNull String name) {
        var lowerCaseInput = name.toLowerCase(Locale.ROOT);
        for (var command : this.registeredCommands) {
            if (command.name().equals(lowerCaseInput) || command.aliases().contains(lowerCaseInput)) {
                return command;
            }
        }
        return null;
    }

    @Override
    public @NonNull Collection<CommandInfo> commands() {
        return Collections.unmodifiableCollection(this.registeredCommands);
    }

    private @NonNull List<String> commandUsageOfRoot(@NonNull String root) {
        List<String> commandUsage = new ArrayList<>();
        for (var command : this.commandManager.commands()) {
            // the first argument is the root, check if it matches
            var arguments = command.components();
            if (arguments.isEmpty() || !arguments.getFirst().name().equalsIgnoreCase(root)) {
                continue;
            }

            commandUsage.add(this.commandManager.commandSyntaxFormatter().apply(null, arguments, null));
        }

        Collections.sort(commandUsage);
        return commandUsage;
    }

}
