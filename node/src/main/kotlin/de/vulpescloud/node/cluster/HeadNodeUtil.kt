package de.vulpescloud.node.cluster

import de.vulpescloud.node.NodeCoroutineScope
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch

object HeadNodeUtil {
    private val heartbeatHooks = mutableListOf<suspend () -> Unit>()
    val nodeHeartbeats: MutableMap<UUID, Duration> = ConcurrentHashMap()

    fun handleHeartbeat(node: build.buf.gen.vulpescloud.node.v1.Node) {
        nodeHeartbeats[UUID.fromString(node.uuid)] = System.currentTimeMillis().milliseconds
        heartbeatHooks.forEach { NodeCoroutineScope.launch { it() } }
    }

    @Suppress("UNUSED")
    fun addHeartbeatHook(block: suspend () -> Unit) {
        heartbeatHooks.add(block)
    }
}
