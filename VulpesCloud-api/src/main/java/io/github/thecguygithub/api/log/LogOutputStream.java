/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.thecguygithub.api.log;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class LogOutputStream extends ByteArrayOutputStream {

    public static final boolean DEFAULT_AUTO_FLUSH = true;
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private final Level level;
    private final Logger logger;
    private final Lock flushLock;

    /**
     * Creates a new log output stream instance.
     *
     * @param level  the level which should be used when logging messages.
     * @param logger the logger to which all messages should get logged.
     * @throws NullPointerException if the given level or logger is null.
     */
    private LogOutputStream(@NonNull Level level, @NonNull Logger logger) {
        this.level = level;
        this.logger = logger;
        this.flushLock = new ReentrantLock();
    }

    /**
     * Creates a new log output stream instance logging all messages to the given logger with the severe log level.
     *
     * @param logger the logger to log all messages to.
     * @return a new log output stream instance for the given logger.
     * @throws NullPointerException if the given logger is null.
     */
    public static @NonNull LogOutputStream forWarn(@NonNull Logger logger) {
        return LogOutputStream.newInstance(logger, Level.WARN);
    }

    /**
     * Creates a new log output stream instance logging all messages to the given logger with the info log level.
     *
     * @param logger the logger to log all messages to.
     * @return a new log output stream instance for the given logger.
     * @throws NullPointerException if the given logger is null.
     */
    public static @NonNull LogOutputStream forInfo(@NonNull Logger logger) {
        return LogOutputStream.newInstance(logger, Level.INFO);
    }

    /**
     * Creates a new log output stream instance logging all messages to the given logger using the given log level.
     *
     * @param logger the logger to log all messages to.
     * @param level  the level which the messages should have when logging them.
     * @return a new log output stream instance for the given logger and level.
     * @throws NullPointerException if the given logger or level is null.
     */
    public static @NonNull LogOutputStream newInstance(@NonNull Logger logger, @NonNull Level level) {
        return new LogOutputStream(level, logger);
    }

    /**
     * Wraps this log output stream into a print stream which automatically flushes and uses the UTF-8 charset encoding.
     *
     * @return a print stream wrapping this logging output stream.
     */
    public @NonNull PrintStream toPrintStream() {
        return this.toPrintStream(DEFAULT_AUTO_FLUSH, DEFAULT_CHARSET);
    }

    /**
     * Wraps this log output stream in a print stream using the given auto flush and charset settings.
     *
     * @param autoFlush if this buffer should be flushed automatically if content was written to the print stream.
     * @param charset   the charset to use when flushing out buffer content.
     * @return a print stream wrapping this output stream.
     * @throws NullPointerException if the given charset is null.
     */
    public @NonNull PrintStream toPrintStream(boolean autoFlush, @NonNull Charset charset) {
        return new PrintStream(this, autoFlush, charset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        // ensure that we only flush once at a time
        this.flushLock.lock();
        try {
            super.flush();
            var content = this.toString(StandardCharsets.UTF_8).stripTrailing();
            super.reset();

            if (!content.isEmpty()) {
                this.logger.atLevel(this.level).log(content);
            }
        } finally {
            this.flushLock.unlock();
        }
    }
}