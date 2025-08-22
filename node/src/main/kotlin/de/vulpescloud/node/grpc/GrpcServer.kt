package de.vulpescloud.node.grpc

import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class GrpcServer(
    private val port: Int = 6565,
    private val services: List<BindableService> = emptyList(),
    private val shutdownTimeoutSec: Long = 5
) {

    private val logger = LoggerFactory.getLogger("gRPC Server")
    @Volatile private var server: Server? = null

    suspend fun start() = withContext(Dispatchers.IO) {
        if (server != null) return@withContext
        server = ServerBuilder.forPort(port).apply {
            services.forEach { addService(it) }
        }.build().start()
        logger.info("gRPC Server started on port $port")
        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking { stop() }
        })
    }

    suspend fun awaitTermination() = withContext(Dispatchers.IO) {
        server?.awaitTermination()
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        val s = server ?: return@withContext
        if (!s.isShutdown) {
            logger.info("gRPC Server shutting down...")
            s.shutdown()
            if (!s.awaitTermination(shutdownTimeoutSec, TimeUnit.SECONDS)) {
                logger.warn("Graceful shutdown timeout, forcing now.")
                s.shutdownNow()
            }
            logger.info("gRPC Server stopped.")
        }
        server = null
    }

    fun serve(
        scope: CoroutineScope,
        context: CoroutineContext = Dispatchers.IO
    ): Job = scope.launch(context) {
        start()
        try {
            awaitTermination()
        } finally {
            withContext(NonCancellable) { stop() }
        }
    }
}