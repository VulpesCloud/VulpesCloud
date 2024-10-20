package io.github.thecguygithub.node.terminal.handler

import lombok.NonNull

abstract class ConsoleTabCompleteHandler : Toggleable() {
    @NonNull
    abstract fun completeInput(@NonNull line: String?): Collection<String?>?
}