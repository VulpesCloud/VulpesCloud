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

package org.vulpesstudios.vulpescloud.bridge.impl.event

import build.buf.gen.vulpescloud.events.v1.event
import build.buf.gen.vulpescloud.events.v1.publishRequest
import build.buf.gen.vulpescloud.events.v1.subscribeRequest
import com.google.protobuf.Any
import com.google.protobuf.Message
import kotlinx.coroutines.*
import org.vulpesstudios.vulpescloud.wrapper.Wrapper
import java.util.*

class EventAPI {

    val eventServiceStub = Wrapper.instance.grpcClient.eventsAPI
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    inline fun <reified T : Message> publish(event: T, broadcast: Boolean = false) {
        scope.launch {
            eventServiceStub.publish(
                publishRequest {
                    this.event = event {
                        this.id =
                            T::class.qualifiedName
                                ?: throw NullPointerException(
                                    "Event class must have a qualified name!"
                                )
                        this.data = Any.pack(event)
                        this.timestamp = Date().time
                    }
                    this.forwardToOtherNodes = broadcast
                }
            )
        }
    }

    inline fun <reified T : Message> subscribe(crossinline handler: suspend (T) -> Unit): Job {
        return scope.launch {
            eventServiceStub
                .subscribe(
                    subscribeRequest {
                        this.eventId =
                            T::class.qualifiedName
                                ?: throw NullPointerException(
                                    "Event class must have a qualified name!"
                                )
                    }
                )
                .collect { handler(it.data.unpack(T::class.java)) }
        }
    }

    fun shutdown() {
        scope.cancel()
    }
}
