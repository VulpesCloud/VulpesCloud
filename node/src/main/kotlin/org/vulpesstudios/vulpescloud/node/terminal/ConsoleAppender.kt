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

package org.vulpesstudios.vulpescloud.node.terminal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import org.vulpesstudios.vulpescloud.node.Node

class ConsoleAppender : ConsoleAppender<ILoggingEvent>() {

    override fun append(eventObject: ILoggingEvent) {
        if (eventObject.level == Level.DEBUG || eventObject.level == Level.TRACE) {
            if (allowedDebugLoggers.contains(eventObject.loggerName)) {
                Node.instance.terminal.print(String(super.encoder.encode(eventObject)))
            }
        } else {
            Node.instance.terminal.print(String(super.encoder.encode(eventObject)))
        }
    }


    companion object {
        private val allowedDebugLoggers: MutableSet<String> = mutableSetOf()

        fun enableDebugLogger(name: String) {
            allowedDebugLoggers.add(name)
        }

        fun disableDebugLogger(name: String) {
            allowedDebugLoggers.remove(name)
        }
    }
}
