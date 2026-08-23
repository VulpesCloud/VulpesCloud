package de.vulpescloud.node.event

import build.buf.gen.vulpescloud.events.v1.Event as GrpcEvent
import build.buf.gen.vulpescloud.events.v1.EventServiceGrpcKt
import build.buf.gen.vulpescloud.events.v1.PublishRequest
import build.buf.gen.vulpescloud.events.v1.PublishResponse
import build.buf.gen.vulpescloud.events.v1.SubscribeRequest
import build.buf.gen.vulpescloud.events.v1.event
import build.buf.gen.vulpescloud.events.v1.publishRequest
import build.buf.gen.vulpescloud.events.v1.subscribeRequest
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.protobuf.Any
import com.google.protobuf.Message
import de.vulpescloud.api.cluster.NodeState
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import de.vulpescloud.node.grpc.security.annotations.RequiresPermission
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class EventsService : EventServiceGrpcKt.EventServiceCoroutineImplBase() {

    private val subscribers: MutableMap<String, MutableList<ProducerScope<GrpcEvent>>> =
        ConcurrentHashMap()
    private val stubCache =
        Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build<String, EventServiceGrpcKt.EventServiceCoroutineStub>()

    fun shutdown() {
        subscribers.forEach { (eventId, scopes) ->
            scopes.forEach { scope ->
                scope.close()
                scopes.remove(scope)
            }
            subscribers.remove(eventId)
        }
    }

    @RequiresPermission("events.subscribe")
    override fun subscribe(request: SubscribeRequest): Flow<GrpcEvent> = callbackFlow {
        subscribers.getOrPut(request.eventId, ::CopyOnWriteArrayList).add(this)

        awaitClose { subscribers[request.eventId]?.remove(this) }
    }

    @RequiresPermission("events.publish")
    override suspend fun publish(request: PublishRequest): PublishResponse {
        val event = request.event
        val localNodeName = Node.instance.configProvider.config.nodeName

        if (request.forwardToOtherNodes) {
            Node.instance.clusterProvider.remoteNodes
                .filter { it.endpoint.name != localNodeName && it.getSnapshot().state == NodeState.ONLINE }
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

        subscribers[event.id]?.forEach { scope -> scope.trySend(event) }

        return PublishResponse.newBuilder().setId(event.id).setAccepted(true).build()
    }

    companion object {
        inline fun <reified T : Message> publish(event: T, broadcast: Boolean = false) {
            NodeCoroutineScope.launch {
                Node.instance.internalEventsService.publish(
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
            return NodeCoroutineScope.launch {
                Node.instance.internalEventsService
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
    }
}
