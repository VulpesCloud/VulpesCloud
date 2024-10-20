package io.github.thecguygithub.node.terminal.handler

abstract class Toggleable {
    private var enabled = true

    fun enabled(): Boolean {
        return this.enabled
    }

    fun enabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun toggle() {
        this.enabled = !this.enabled
    }
}