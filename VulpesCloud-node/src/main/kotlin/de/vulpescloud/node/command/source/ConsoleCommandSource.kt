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