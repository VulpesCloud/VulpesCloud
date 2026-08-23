package de.vulpescloud.api.services

import build.buf.gen.vulpescloud.services.v1.ServiceDefinition
import build.buf.gen.vulpescloud.services.v1.ServiceState
import com.google.protobuf.Timestamp
import de.vulpescloud.api.serializer.TimestampSerializer
import de.vulpescloud.api.serializer.UUIDSerializer
import de.vulpescloud.api.tasks.Task
import java.util.*
import kotlinx.serialization.Serializable

@Serializable
data class Service(
    val task: Task,
    @Serializable(UUIDSerializer::class) val uuid: UUID,
    val orderedId: Int,
    val port: Int,
    val node: String,
    val playerCount: Int,
    @Serializable(TimestampSerializer::class) val startTime: Timestamp,
    val state: ServiceStates,
    val hostname: String,
    val metadata: Map<String, String> = emptyMap(),
) {
    fun name(): String = task.name + "-" + orderedId

    fun toDefinition(): ServiceDefinition =
        ServiceDefinition.newBuilder()
            .setTask(task.toDefinition())
            .setUuid(uuid.toString())
            .setOrderedId(orderedId)
            .setPort(port)
            .setNode(node)
            .setPlayerCount(playerCount)
            .setStartTime(startTime)
            .setState(
                when (state) {
                    ServiceStates.UNKNOWN -> ServiceState.SERVICE_STATE_UNSPECIFIED
                    ServiceStates.PREPARED -> ServiceState.SERVICE_STATE_PREPARED
                    ServiceStates.STARTING -> ServiceState.SERVICE_STATE_STARTING
                    ServiceStates.RUNNING -> ServiceState.SERVICE_STATE_RUNNING
                    ServiceStates.STOPPED -> ServiceState.SERVICE_STATE_STOPPED
                }
            )
            .setHostname(hostname)
            .putAllMetadata(metadata)
            .build()

    companion object {
        fun fromDefinition(definition: ServiceDefinition): Service {
            return Service(
                Task.fromDefinition(definition.task),
                UUID.fromString(definition.uuid),
                definition.orderedId,
                definition.port,
                definition.node,
                definition.playerCount,
                definition.startTime,
                when (definition.state) {
                    ServiceState.SERVICE_STATE_UNSPECIFIED -> ServiceStates.UNKNOWN
                    ServiceState.SERVICE_STATE_PREPARED -> ServiceStates.PREPARED
                    ServiceState.SERVICE_STATE_STARTING -> ServiceStates.STARTING
                    ServiceState.SERVICE_STATE_RUNNING -> ServiceStates.RUNNING
                    ServiceState.SERVICE_STATE_STOPPED -> ServiceStates.STOPPED
                    ServiceState.UNRECOGNIZED -> ServiceStates.UNKNOWN
                },
                definition.hostname,
                definition.metadataMap,
            )
        }
    }
}
