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

package org.vulpesstudios.vulpescloud.node.event

import build.buf.gen.vulpescloud.events.v1.*
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.protobuf.Any
import com.google.protobuf.Message
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.cluster.NodeState
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.NodeCoroutineScope
import org.vulpesstudios.vulpescloud.node.grpc.security.AuthClientInterceptor
import org.vulpesstudios.vulpescloud.node.grpc.security.annotations.RequiresPermission
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import build.buf.gen.vulpescloud.events.v1.Event as GrpcEvent

class EventsService : EventServiceGrpcKt.EventServiceCoroutineImplBase() {

    private val subscribers: MutableMap<String, MutableList<ProducerScope<GrpcEvent>>> =
        ConcurrentHashMap()
    private val stubCache =
        Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build<String, EventServiceGrpcKt.EventServiceCoroutineStub>()

    private val publishChannel = Channel<PublishRequest>(Channel.UNLIMITED)
    private val nodeChannels: MutableMap<String, Channel<PublishRequest>> = ConcurrentHashMap()
    private val nodeChannelsMutex = Mutex()

    init {
        NodeCoroutineScope.launch {
            for (request in publishChannel) {
                try {
                    processPublish(request)
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        ensureActive()
                    }
                    logger.error("Failed to process event publish", e)
                }
            }
        }
    }

    fun shutdown() {
        publishChannel.close()
        nodeChannels.values.forEach { it.close() }
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

    private suspend fun processPublish(request: PublishRequest) {
        val event = request.event

        // Notify local subscribers immediately - don't wait for remote forwards
        subscribers[event.id]?.forEach { scope -> scope.trySend(event) }

        // Forward to remote nodes via per-node channels to preserve order
        if (request.forwardToOtherNodes) {
            val localNodeName = Node.instance.configProvider.config.nodeName
            Node.instance.clusterProvider.remoteNodes
                .filter { it.endpoint.name != localNodeName && it.getSnapshot().state == NodeState.ONLINE }
                .forEach { node ->
                    val channel = getOrCreateNodeChannel(node.endpoint.name)
                    channel.trySend(request)
                }
        }
    }

    private suspend fun getOrCreateNodeChannel(nodeName: String): Channel<PublishRequest> =
        nodeChannelsMutex.withLock {
            nodeChannels.getOrPut(nodeName) {
                val ch = Channel<PublishRequest>(Channel.UNLIMITED)
                NodeCoroutineScope.launch {
                    for (req in ch) {
                        try {
                            val node = Node.instance.clusterProvider.remoteNodes.find { it.endpoint.name == nodeName }
                                ?: continue
                            val nodeChannel = node.channel ?: continue
                            val stub =
                                stubCache.get(nodeName) {
                                    EventServiceGrpcKt.EventServiceCoroutineStub(nodeChannel)
                                        .withInterceptors(AuthClientInterceptor(Node.instance.secret))
                                }
                            stub.publish(
                                PublishRequest.newBuilder()
                                    .setEvent(req.event)
                                    .setForwardToOtherNodes(false)
                                    .build()
                            )
                        } catch (e: Exception) {
                            if (e is CancellationException) {
                                ensureActive()
                            }
                            logger.error("Failed to forward event to $nodeName", e)
                        }
                    }
                }
                ch
            }
        }

    @RequiresPermission("events.publish")
    override suspend fun publish(request: PublishRequest): PublishResponse {
        publishChannel.send(request)
        return PublishResponse.newBuilder().setId(request.event.id).setAccepted(true).build()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(EventsService::class.java)

        suspend inline fun <reified T : Message> publish(event: T, broadcast: Boolean = false) {
            logger.trace("Publishing event {} with broadcast {} and Data: \n {}", event::class.qualifiedName, broadcast, event)
            Node.instance.internalEventsService.publish(
                publishRequest {
                    this.event = event {
                        this.id = event.descriptorForType.fullName
                        this.data = Any.pack(event)
                        this.timestamp = Date().time
                    }
                    this.forwardToOtherNodes = broadcast
                }
            )
        }

        inline fun <reified T : Message> subscribe(crossinline handler: suspend (T) -> Unit): Job {
            logger.trace("Subscribing to event ${T::class.qualifiedName}")
            return NodeCoroutineScope.launch {
                Node.instance.internalEventsService
                    .subscribe(
                        subscribeRequest {
                            this.eventId = eventName<T>()
                        }
                    )
                    .collect { handler(it.data.unpack(T::class.java)) }
            }
        }

        inline fun <reified T : Message> eventName(): String =
            T::class.java.getMethod("getDefaultInstance").invoke(null)
                .let { (it as Message).descriptorForType.fullName }
    }
}
