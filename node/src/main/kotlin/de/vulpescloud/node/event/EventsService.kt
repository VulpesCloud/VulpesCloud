package de.vulpescloud.node.event

import build.buf.gen.vulpescloud.events.v1.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import java.util.concurrent.CopyOnWriteArrayList

class EventsService : EventServiceGrpcKt.EventServiceCoroutineImplBase() {

    private val subscribers = CopyOnWriteArrayList<Channel<Event>>()

    override fun subscribe(request: SubscribeRequest): Flow<Event> {
        val channel = Channel<Event>(capacity = Channel.UNLIMITED)
        subscribers.add(channel)

        return channel.consumeAsFlow()
    }

    override suspend fun publish(request: PublishRequest): PublishResponse {
        val event = request.event

        subscribers.forEach { channel -> channel.trySend(event).isSuccess }

        return PublishResponse.newBuilder().setId(event.id).setAccepted(true).build()
    }
}
