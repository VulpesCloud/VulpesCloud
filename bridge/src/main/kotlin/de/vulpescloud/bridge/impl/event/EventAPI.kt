package de.vulpescloud.bridge.impl.event

import build.buf.gen.vulpescloud.events.v1.PublishRequest
import build.buf.gen.vulpescloud.events.v1.SubscribeRequest
import de.vulpescloud.api.events.Event
import de.vulpescloud.api.events.EventSerializer
import de.vulpescloud.wrapper.Wrapper
import kotlinx.coroutines.*
import java.util.*
import build.buf.gen.vulpescloud.events.v1.Event as GrpcEvent

class EventAPI {

    val eventServiceStub = Wrapper.instance.grpcClient.eventsAPI
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(DelicateCoroutinesApi::class)
    fun publish(event: GrpcEvent, forwardToOtherNodes: Boolean = false) {
        GlobalScope.launch {
            eventServiceStub.publish(
                PublishRequest.newBuilder()
                    .setEvent(event)
                    .setForwardToOtherNodes(forwardToOtherNodes)
                    .build()
            )
        }
    }

    //    @OptIn(DelicateCoroutinesApi::class)
    //    fun <T> subscribe(clazz: Class<T>, handler: Consumer<Event<T>>) {
    //        GlobalScope.launch {
    //            eventServiceStub.subscribe(
    //                build.buf.gen.vulpescloud.events.v1.SubscribeRequest.newBuilder().build()
    //            ).collect { grpcEvent ->
    //                // Compare with clazz.name instead of clazz.kotlin.qualifiedName
    //                if (grpcEvent.type == clazz.name) {
    //                    val decoded = EventSerializer.decode<T>(grpcEvent)
    //                    val wrapped = Event(
    //                        id = UUID.fromString(grpcEvent.id),
    //                        type = grpcEvent.type,
    //                        metadata = grpcEvent.metadataMap,
    //                        event = decoded,
    //                        timestamp = grpcEvent.timestamp,
    //                    )
    //                    handler.accept(wrapped)
    //                }
    //            }
    //        }
    //    }

    inline fun <reified T> publish(
        event: T,
        forwardToOtherNodes: Boolean = false,
        metadata: Map<String, String> = emptyMap(),
    ) {
        val grpcEvent = EventSerializer.encode(event, metadata)
        publish(grpcEvent, forwardToOtherNodes)
    }

    @OptIn(DelicateCoroutinesApi::class)
    inline fun <reified T> subscribe(noinline handler: suspend (Event<T>) -> Unit): Job {
        return scope.launch {
            eventServiceStub.subscribe(SubscribeRequest.newBuilder().build()).collect { grpcEvent ->
                if (grpcEvent.type == T::class.qualifiedName) {
                    val decoded = EventSerializer.decode<T>(grpcEvent)
                    val wrapped =
                        Event(
                            id = UUID.fromString(grpcEvent.id),
                            type = grpcEvent.type,
                            metadata = grpcEvent.metadataMap,
                            event = decoded,
                            timestamp = grpcEvent.timestamp,
                        )
                    handler(wrapped)
                }
            }
        }
    }
}
