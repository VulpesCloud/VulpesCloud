package de.vulpescloud.node.grpc

import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.AuthInterceptor
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class GrpcServer(
    private val host: String = "127.0.0.1",
    private val port: Int = 6565,
    private val services: List<BindableService> = emptyList(),
    private val interceptors: List<io.grpc.ServerInterceptor> = emptyList(),
    private val shutdownTimeoutSec: Long = 5
) {
    private val logger = LoggerFactory.getLogger("gRPC Server")
    @Volatile
    private var server: Server? = null

    suspend fun start() = withContext(Dispatchers.IO) {
        if (server?.isTerminated == false) {
            logger.warn("Server already running")
            return@withContext
        }

        try {
            val certFile = File("certs/server.crt")
            val keyFile = File("certs/server.key")

            if (!certFile.exists() || !keyFile.exists()) {
                throw IllegalStateException("TLS Certs are missing: ${certFile.path}, ${keyFile.path}")
            }

            val address = InetSocketAddress(host, port)
            server = NettyServerBuilder.forAddress(address).apply {
                services.forEach { svc ->
                    var def = svc.bindService()
                    interceptors.forEach { def = ServerInterceptors.intercept(def, it) }
                    addService(def)
                }
                useTransportSecurity(certFile, keyFile)
            }.build().start()
            logger.info("gRPC Server started on $address")
        } catch (ex: IOException) {
            logger.error("Failed to start gRPC Server", ex)
            throw ex
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                runBlocking(Dispatchers.IO) { stop() }
            } catch (ex: Exception) {
                logger.error("Error during shutdown hook", ex)
            }
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
        try {
            start()
            awaitTermination()
        } finally {
            withContext(NonCancellable) { stop() }
        }
    }
}