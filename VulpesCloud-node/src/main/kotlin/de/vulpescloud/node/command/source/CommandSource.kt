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

import de.vulpescloud.api.Named
import lombok.NonNull

interface CommandSource : Named {
    /**
     * @param message the message that is sent to the source
     * @throws NullPointerException if message is null.
     */
    fun sendMessage(@NonNull message: String?)

    /**
     * @param messages the messages that are sent to the source
     * @throws NullPointerException if messages is null.
     */
    fun sendMessage(@NonNull vararg messages: String?)

    /**
     * @param messages the messages that are sent to the source
     * @throws NullPointerException if messages is null.
     */
    fun sendMessage(@NonNull messages: Collection<String?>?)

    /**
     * Used to check if the command source has the given permission
     *
     * @param permission the permission to check for
     * @return whether the source has the permission
     * @throws NullPointerException if permission is null.
     */
    fun checkPermission(@NonNull permission: String?): Boolean

    companion object {
        /**
         * @return the console command source instance
         */
        @NonNull
        fun console(): CommandSource {
            return ConsoleCommandSource.INSTANCE
        }
    }
}