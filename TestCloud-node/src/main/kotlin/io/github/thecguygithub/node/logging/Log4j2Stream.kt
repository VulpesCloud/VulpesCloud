package io.github.thecguygithub.node.logging

import org.jetbrains.annotations.Contract
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream
import java.nio.charset.StandardCharsets


class Log4j2Stream : ByteArrayOutputStream() {

    private val callback: LoggingCallback? = null

    @Throws(IOException::class)
    override fun flush() {
        super.flush()

        val input = this.toString(StandardCharsets.UTF_8)
        super.reset()

        if (input != null && input.isNotEmpty()) {
            callback!!.print(input.replace("\n", ""))
        }
    }

    @Contract(" -> new")
    fun printStream(): PrintStream {
        return PrintStream(this, true, StandardCharsets.UTF_8)
    }
}