package de.vulpescloud.node.event

import build.buf.gen.vulpescloud.events.v1.Event as GrpcEvent
import build.buf.gen.vulpescloud.events.v1.EventServiceGrpcKt
import build.buf.gen.vulpescloud.events.v1.PublishRequest
import build.buf.gen.vulpescloud.events.v1.PublishResponse
import build.buf.gen.vulpescloud.events.v1.SubscribeRequest
import com.github.benmanes.caffeine.cache.Caffeine
import de.vulpescloud.api.events.Event
import de.vulpescloud.api.events.EventSerializer
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.cluster.ClusterHelper
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import de.vulpescloud.node.grpc.security.annotations.RequiresPermission
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
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
    private val logger = LoggerFactory.getLogger(EventsService::class.java)
    private val stubCache =
        Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build<String, EventServiceGrpcKt.EventServiceCoroutineStub>()

    @RequiresPermission("events.subscribe")
    override fun subscribe(request: SubscribeRequest): Flow<GrpcEvent> {
        val channel = Channel<GrpcEvent>(capacity = Channel.UNLIMITED)
        subscribers.add(channel)

        return channel.consumeAsFlow().onCompletion { cause ->
            subscribers.remove(channel)
            channel.close()
            when {
                cause is CancellationException ->
                    logger.debug("Client cancelled subscription normally")
                cause != null -> logger.error("Subscription ended with error", cause)
            }
        }
    }

    @RequiresPermission("events.publish")
    override suspend fun publish(request: PublishRequest): PublishResponse {
        val event = request.event
        val localNodeName = ClusterHelper.getLocalNode().name

        if (request.forwardToOtherNodes) {
            Node.instance.clusterProvider.remoteNodes
                .filter { it.endpoint.name != localNodeName && it.getNode().isRunning() }
                .forEach { node ->
                    val channel = node.channel ?: return@forEach
                    val stub =
                        stubCache.get(node.endpoint.name) {
                            EventServiceGrpcKt.EventServiceCoroutineStub(channel)
                                .withInterceptors(AuthClientInterceptor(Node.instance.secret))
                        }
                    stub.publish(
                        PublishRequest.newBuilder()
                            .setEvent(event)
                            .setForwardToOtherNodes(false)
                            .build()
                    )
                }
        }

        subscribers.forEach { it.trySend(event) }

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
                Node.instance.localGrpcClient.eventsAPI
                    .subscribe(SubscribeRequest.newBuilder().build())
                    .collect { grpcEvent ->
                        if (grpcEvent.type == T::class.qualifiedName) {
                            handler(
                                Event(
                                    id = UUID.fromString(grpcEvent.id),
                                    type = grpcEvent.type,
                                    metadata = grpcEvent.metadataMap,
                                    event = EventSerializer.decode<T>(grpcEvent),
                                    timestamp = grpcEvent.timestamp,
                                )
                            )
                        }
                    }
            }
        }
    }
}
