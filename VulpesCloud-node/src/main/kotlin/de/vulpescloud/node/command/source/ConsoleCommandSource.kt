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

package de.vulpescloud.node.command.source

import lombok.NonNull
import org.slf4j.LoggerFactory

class ConsoleCommandSource : CommandSource {
    /**
     * {@inheritDoc}
     */
    @NonNull
    override fun name(): String {
        return "Console"
    }

    /**
     * {@inheritDoc}
     */
    override fun sendMessage(@NonNull message: String?) {
        LOGGER.info(message!!)
    }

    /**
     * {@inheritDoc}
     */
    override fun sendMessage(vararg messages: String?) {
        for (message in messages) {
            LOGGER.info(message!!)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun sendMessage(@NonNull messages: Collection<String?>?) {
        for (message in messages!!) {
            LOGGER.info(message!!)
        }
    }

    /**
     * @param permission the permission to check for
     * @return always true as the console is allowed to execute every command
     * @throws NullPointerException if permission is null.
     */
    override fun checkPermission(@NonNull permission: String?): Boolean {
        return true
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    override fun toString(): String {
        return this.name()
    }

    companion object {
        val INSTANCE: ConsoleCommandSource = ConsoleCommandSource()
        private val LOGGER = LoggerFactory.getLogger(ConsoleCommandSource::class.java)
    }
}