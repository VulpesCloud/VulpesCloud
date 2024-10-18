package io.github.thecguygithub.node.command;

import io.github.thecguygithub.api.command.CommandInfo;
import io.github.thecguygithub.node.command.source.CommandSource;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CommandProvider {

    @NonNull
    List<String> suggest(@NonNull CommandSource source, @NonNull String input);

    CompletableFuture<?> execute(@NonNull CommandSource source, @NonNull String input);

    void register(@NonNull Class<?> commandClass);

    void register(@NonNull Object command);

    @Nullable
    CommandInfo command(@NonNull String name);

    @UnmodifiableView
    @NonNull
    Collection<CommandInfo> commands();

}
