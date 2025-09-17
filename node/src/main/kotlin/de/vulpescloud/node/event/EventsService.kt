package de.vulpescloud.node.event

import build.buf.gen.vulpescloud.events.v1.EventServiceGrpcKt
import build.buf.gen.vulpescloud.events.v1.PublishRequest
import build.buf.gen.vulpescloud.events.v1.PublishResponse
import build.buf.gen.vulpescloud.events.v1.SubscribeRequest
import de.vulpescloud.api.events.Event
import de.vulpescloud.api.events.EventSerializer
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import build.buf.gen.vulpescloud.events.v1.Event as GrpcEvent

class EventsService : EventServiceGrpcKt.EventServiceCoroutineImplBase() {

    private val subscribers = CopyOnWriteArrayList<Channel<GrpcEvent>>()

    override fun subscribe(request: SubscribeRequest): Flow<GrpcEvent> {
        val channel = Channel<GrpcEvent>(capacity = Channel.UNLIMITED)
        subscribers.add(channel)

        return channel.consumeAsFlow()
    }

    override suspend fun publish(request: PublishRequest): PublishResponse {
        val event = request.event

        subscribers.forEach { channel -> channel.trySend(event).isSuccess }

        return PublishResponse.newBuilder().setId(event.id).setAccepted(true).build()
    }

    companion object {
        fun publish(event: GrpcEvent) {
            NodeCoroutineScope.launch {
                Node.instance.localGrpcClient.eventsAPI.publish(
                    PublishRequest.newBuilder().setEvent(event).build()
                )
            }
        }

        inline fun <reified T> subscribe(noinline handler: suspend (Event<T>) -> Unit) {
            NodeCoroutineScope.launch {
                val request = SubscribeRequest.newBuilder().build()

                Node.instance.localGrpcClient.eventsAPI.subscribe(request).collect { grpcEvent ->
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
}
