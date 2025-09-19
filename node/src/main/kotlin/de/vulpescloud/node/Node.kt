package de.vulpescloud.node

import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import de.vulpescloud.node.command.CommandProvider
import de.vulpescloud.node.commands.*
import de.vulpescloud.node.config.ConfigProvider
import de.vulpescloud.node.event.EventsService
import de.vulpescloud.node.grpc.GrpcServer
import de.vulpescloud.node.grpc.LocalGrpcClient
import de.vulpescloud.node.grpc.LoggingServerInterceptor
import de.vulpescloud.node.grpc.security.AuthInterceptor
import de.vulpescloud.node.secret.SecretFactory
import de.vulpescloud.node.services.AbstractService
import de.vulpescloud.node.services.ServiceFactoryProvider
import de.vulpescloud.node.services.ServicesAPIService
import de.vulpescloud.node.services.impl.docker.DockerServiceFactory
import de.vulpescloud.node.services.impl.local.LocalServiceFactory
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.setup.setups.FirstSetup
import de.vulpescloud.node.tasks.TasksAPIService
import de.vulpescloud.node.templates.TemplateStorageProvider
import de.vulpescloud.node.terminal.Terminal
import io.grpc.ChannelCredentials
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

class Node {
    private val logger = LoggerFactory.getLogger("Node")

    val terminal = Terminal()
    val commandProvider = CommandProvider()
    val configProvider = ConfigProvider()
    lateinit var mongoClient: MongoClient
    lateinit var secret: String
    lateinit var setupProvider: SetupProvider
    lateinit var creds: ChannelCredentials
    var inputJob: Job? = null
        private set

    lateinit var dockerClientConfig: DockerClientConfig
    lateinit var dockerHttpClient: DockerHttpClient
    val templateStorageProvider = TemplateStorageProvider()
    val localGrpcClient = LocalGrpcClient()
    val serviceFactoryProvider = ServiceFactoryProvider()

    val nodeServices = mutableListOf<AbstractService>()

    suspend fun init(scope: CoroutineScope) =
        withContext(Dispatchers.IO) {
            instance = this@Node

            val configExists = configProvider.loadConfig()

            setupProvider = SetupProvider(terminal)
            terminal.init()

            //            CertGen.loadOrCreate(
            //                keyFile = File("certs/server.key"),
            //                certFile = File("certs/server.crt"),
            //            )

            //            val serverCertBytes = File("certs/server.crt")
            //            creds =
            // TlsChannelCredentials.newBuilder().trustManager(serverCertBytes).build()

            val secretFactory = SecretFactory()
            secret = secretFactory.loadOrCreateSecret(Path("launcher/secret/.auth.secret"))

            inputJob = scope.launch(Dispatchers.IO) { terminal.allowInput() }

            if (!configExists) {
                setupProvider.startSetup(FirstSetup())
            }

            while (setupProvider.currentSetup?.setup is FirstSetup) {
                delay(500)
            }

            terminal.changePrompt("")

            val grpcServer =
                GrpcServer(
                    host = configProvider.config.grpcHost,
                    port = configProvider.config.grpcPort,
                    services = listOf(TasksAPIService(), ServicesAPIService(), EventsService()),
                    interceptors = listOf(LoggingServerInterceptor(), AuthInterceptor(secret)),
                )
            grpcServer.start()
            NodeCoroutineScope.launch { grpcServer.awaitTermination() }

            localGrpcClient.connect(
                host = configProvider.config.grpcHost,
                port = configProvider.config.grpcPort,
                secret = secret,
            )

            try {
                commandProvider.initialize()
                commandProvider.apply {
                    register(ClearCommand(terminal))
                    register(HelpCommand(commandProvider))
                    register(ExitCommand())
                    register(InfoCommand())
                    register(ServiceCommand())
                    register(DebugCommand())
                }
            } catch (e: Exception) {
                logger.error("Failed to initialize commands: ${e.stackTraceToString()}")
                return@withContext
            }

            val connectionString = configProvider.config.mongodb.connectionString
            try {
                val settings =
                    MongoClientSettings.builder()
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

            serviceFactoryProvider.apply {
                registerServiceFactory(DockerServiceFactory())
                registerServiceFactory(LocalServiceFactory())
            }

            if (configProvider.config.docker.enabled) {
                try {
                    dockerClientConfig =
                        DefaultDockerClientConfig.createDefaultConfigBuilder()
                            .withDockerHost(configProvider.config.docker.host)
                            .withDockerCertPath(configProvider.config.docker.dockerCertPath)
                            .withDockerTlsVerify(
                                configProvider.config.docker.dockerCertPath != null
                            )
                            .withRegistryUsername(configProvider.config.docker.registryUsername)
                            .withRegistryPassword(configProvider.config.docker.registryPassword)
                            .withRegistryEmail(configProvider.config.docker.registryEmail)
                            .withRegistryUrl("https://index.docker.io/v1/")
                            .build()

                    dockerHttpClient =
                        ApacheDockerHttpClient.Builder()
                            .dockerHost(dockerClientConfig.dockerHost)
                            .sslConfig(dockerClientConfig.sslConfig)
                            .maxConnections(100)
                            .connectionTimeout(Duration.ofSeconds(30))
                            .responseTimeout(Duration.ofSeconds(45))
                            .build()

                    logger.info("Trying to connect to Docker...")
                    val dockerClient =
                        DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient)
                    val version = dockerClient.versionCmd().exec()
                    logger.info(
                        "Successfully connected to Docker! Version: ${version.version}, API Version: ${version.apiVersion}"
                    )

                    // build docker image

                } catch (e: Exception) {
                    logger.error("Failed to connect to Docker: ${e.message}")
                    return@withContext
                }
            }

            // Runtime.getRuntime().addShutdownHook(Thread { NodeShutdown.shutdown() })
        }

    fun getVelocitySecret(): String {
        val secretFactory = SecretFactory()
        return secretFactory.loadOrCreateSecret(Path("launcher/secret/.velocity.secret"))
    }

    companion object {
        lateinit var instance: Node

        suspend fun create(scope: CoroutineScope): Node {
            val node = Node()
            node.init(scope)
            return node
        }

        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            val node = create(this)
            node.inputJob?.join()
        }
    }
}
