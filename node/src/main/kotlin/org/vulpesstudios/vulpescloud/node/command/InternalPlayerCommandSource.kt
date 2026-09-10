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

import org.vulpesstudios.vulpescloud.node.grpc.security.model.UserModel
import java.util.concurrent.ConcurrentLinkedQueue

class InternalPlayerCommandSource(val user: UserModel) : CommandSource {
    val messages = ConcurrentLinkedQueue<String>()

    override fun sendMessage(message: String) {
        messages.add(message)
    }

    override fun sendError(message: String) {
        messages.add(message)
    }
}
