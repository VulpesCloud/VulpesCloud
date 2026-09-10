/*
 * Copyright 2024-2026 VulpesStudios & Contributers
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

package org.vulpesstudios.vulpescloud.node.command

import kotlinx.coroutines.runBlocking
import org.incendo.cloud.CommandManager
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.internal.CommandRegistrationHandler
import org.vulpesstudios.vulpescloud.node.grpc.security.PermissionHelper

class CloudCommandManager :
    CommandManager<CommandSource>(
        ExecutionCoordinator.asyncCoordinator(),
        CommandRegistrationHandler.nullCommandRegistrationHandler(),
    ) {

    override fun hasPermission(sender: CommandSource, permission: String): Boolean {
        return runBlocking {
            return@runBlocking when (sender) {
                is ConsoleCommandSource -> true
                is InternalPlayerCommandSource ->
                    PermissionHelper.hasPermission(sender.user.name, permission)
                else -> throw UnsupportedOperationException()
            }
        }
    }
}
