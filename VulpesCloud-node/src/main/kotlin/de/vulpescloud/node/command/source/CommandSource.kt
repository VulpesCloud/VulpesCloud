/*
 * Copyright 2019-2024 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is copied from CloudNet(https://github.com/CloudNetService/CloudNet)
 * and is NOT made by me! <3 CloudNet-Team
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