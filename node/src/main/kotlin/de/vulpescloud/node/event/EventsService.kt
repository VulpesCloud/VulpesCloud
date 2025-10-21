package de.vulpescloud.node.event

import build.buf.gen.vulpescloud.events.v1.Event as GrpcEvent
import build.buf.gen.vulpescloud.events.v1.EventServiceGrpcKt
import build.buf.gen.vulpescloud.events.v1.PublishRequest
import build.buf.gen.vulpescloud.events.v1.PublishResponse
import build.buf.gen.vulpescloud.events.v1.SubscribeRequest
import de.vulpescloud.api.events.Event
import de.vulpescloud.api.events.EventSerializer
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class EventsService : EventServiceGrpcKt.EventServiceCoroutineImplBase() {

    private val subscribers = CopyOnWriteArrayList<Channel<GrpcEvent>>()
    private val logger = LoggerFactory.getLogger("EventsService")

    override fun subscribe(request: SubscribeRequest): Flow<GrpcEvent> {
        val channel = Channel<GrpcEvent>(capacity = Channel.UNLIMITED)
        subscribers.add(channel)

        return channel.consumeAsFlow().onCompletion { cause ->
            subscribers.remove(channel)
            channel.close()
            if (cause is CancellationException) {
                logger.debug("Client cancelled subscription normally")
            } else if (cause != null) {
                logger.error("Subscription ended with error", cause)
            }
        }
    }

    override suspend fun publish(request: PublishRequest): PublishResponse {
        val event = request.event

        if (request.forwardToOtherNodes) {
            Node.instance.clusterProvider.remoteNodes.forEach { node ->
                val stub =
                    EventServiceGrpcKt.EventServiceCoroutineStub(node.channel ?: return@forEach)
                stub.publish(
                    PublishRequest.newBuilder()
                        .setEvent(event)
                        .setForwardToOtherNodes(false)
                        .build()
                )
            }
        }

        subscribers.forEach { channel -> channel.trySend(event).isSuccess }

        return PublishResponse.newBuilder().setId(event.id).setAccepted(true).build()
    }

    companion object {
        fun publish(event: GrpcEvent, broadcast: Boolean = false) {
            NodeCoroutineScope.launch {
                Node.instance.localGrpcClient.eventsAPI.publish(
                    PublishRequest.newBuilder()
                        .setEvent(event)
                        .setForwardToOtherNodes(broadcast)
                        .build()
                )
            }
        }

        inline fun <reified T> subscribe(noinline handler: suspend (Event<T>) -> Unit): Job {
            return NodeCoroutineScope.launch {
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
