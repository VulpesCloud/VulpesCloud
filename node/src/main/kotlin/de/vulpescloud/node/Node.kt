package de.vulpescloud.node

import build.buf.gen.vulpescloud.node.v1.CreateServiceRequest
import build.buf.gen.vulpescloud.node.v1.NodeServiceGrpcKt
import build.buf.gen.vulpescloud.node.v1.TaskDefinition
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import de.vulpescloud.node.command.CommandProvider
import de.vulpescloud.node.commands.ClearCommand
import de.vulpescloud.node.commands.ExitCommand
import de.vulpescloud.node.commands.HelpCommand
import de.vulpescloud.node.commands.InfoCommand
import de.vulpescloud.node.config.ConfigProvider
import de.vulpescloud.node.grpc.GrpcServer
import de.vulpescloud.node.grpc.LoggingServerInterceptor
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import de.vulpescloud.node.grpc.security.AuthInterceptor
import de.vulpescloud.node.grpc.security.CertGen
import de.vulpescloud.node.grpc.services.NodeServiceImpl
import de.vulpescloud.node.secret.SecretFactory
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.terminal.Terminal
import io.grpc.Grpc
import io.grpc.StatusException
import io.grpc.TlsChannelCredentials
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

class Node {
    private val logger = LoggerFactory.getLogger("Node")

    val terminal = Terminal()
    val commandProvider = CommandProvider()
    val configProvider = ConfigProvider()
    lateinit var mongoClient: MongoClient
    lateinit var secret: String
    val setupProvider = SetupProvider()

    suspend fun init() = withContext(Dispatchers.IO) {
        instance = this@Node

        terminal.init()

        setupProvider.init()

        CertGen.loadOrCreate(
            keyFile = File("certs/server.key"),
            certFile = File("certs/server.crt")
        )

        val secretFactory = SecretFactory()
        configProvider.loadConfig()
        secret = secretFactory.loadOrCreateSecret(Path("launcher/secret/.auth.secret"))

        val grpcServer = GrpcServer(
            port = configProvider.config.grpcPort,
            services = listOf(
                NodeServiceImpl()
            ),
            interceptors = listOf(
                LoggingServerInterceptor(),
                AuthInterceptor(secret)
            )
        )

        grpcServer.start()
        NodeCoroutineScope.launch { grpcServer.awaitTermination() }

        commandProvider.initialize()
        commandProvider.apply {
            register(ClearCommand(terminal))
            register(HelpCommand(commandProvider))
            register(ExitCommand())
            register(InfoCommand())
        }

        val connectionString = configProvider.config.mongodb.connectionString

        try {
            val settings = MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(connectionString))
                .applyToConnectionPoolSettings {
                    it.maxSize(50)
                    it.maxWaitTime(2, TimeUnit.SECONDS)
                }
                .applyToSocketSettings {
                    it.connectTimeout(2, TimeUnit.SECONDS)
                    it.readTimeout(2, TimeUnit.SECONDS)
                }
                .retryWrites(true)
                .build()

            mongoClient = MongoClient.create(settings)
            logger.info("Successfully connected to MongoDB!")
        } catch (e: Exception) {
            logger.error("Failed to connect to MongoDB: ${e.message}")
            return@withContext
        }

        val serverCertBytes = File("certs/server.crt")

        val creds = TlsChannelCredentials.newBuilder()
            .trustManager(serverCertBytes)
            .build()

        val channel = Grpc.newChannelBuilderForAddress(
            "127.0.0.1",
            configProvider.config.grpcPort,
            creds
        ).build()

        val stub = NodeServiceGrpcKt.NodeServiceCoroutineStub(channel)
            .withInterceptors(AuthClientInterceptor(secret))

        delay(1500)
        try {
            val response = stub.createService(
                CreateServiceRequest.newBuilder()
                    .setTask(TaskDefinition.newBuilder().setName("test").build())
                    .build()
            )
            logger.info("createService OK: $response")
        } catch (e: StatusException) {
            logger.error("RPC createService failed: status=${e.status} cause=${e.cause}", e)
        } catch (t: Throwable) {
            logger.error("Unexpected error calling createService", t)
        }
    }

    fun startInput(scope: CoroutineScope): Job =
        scope.launch(Dispatchers.IO) { terminal.allowInput() }

    companion object {
        lateinit var instance: Node

        suspend fun create(scope: CoroutineScope): Pair<Node, Job> {
            val node = Node()
            node.init()
            val inputJob = node.startInput(scope)
            return node to inputJob
        }

        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            val (_, inputJob) = create(this)
            inputJob.join()
        }
    }
}