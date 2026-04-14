package de.vulpescloud.bridge.impl.event

import build.buf.gen.vulpescloud.events.v1.event
import build.buf.gen.vulpescloud.events.v1.publishRequest
import build.buf.gen.vulpescloud.events.v1.subscribeRequest
import com.google.protobuf.Any
import com.google.protobuf.Message
import de.vulpescloud.wrapper.Wrapper
import java.util.*
import kotlinx.coroutines.*

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
